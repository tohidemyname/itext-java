/*
    This file is part of the iText (R) project.
    Copyright (c) 1998-2024 Apryse Group NV
    Authors: Apryse Software.

    This program is offered under a commercial and under the AGPL license.
    For commercial licensing, contact us at https://itextpdf.com/sales.  For AGPL licensing, see below.

    AGPL licensing:
    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.itextpdf.signatures.validation.v1;

import com.itextpdf.bouncycastleconnector.BouncyCastleFactoryCreator;
import com.itextpdf.commons.bouncycastle.IBouncyCastleFactory;
import com.itextpdf.commons.bouncycastle.cert.ocsp.IBasicOCSPResp;
import com.itextpdf.commons.bouncycastle.cert.ocsp.ISingleResp;
import com.itextpdf.commons.utils.DateTimeUtil;
import com.itextpdf.commons.utils.MessageFormatUtil;
import com.itextpdf.signatures.CertificateUtil;
import com.itextpdf.signatures.CrlClientOnline;
import com.itextpdf.signatures.ICrlClient;
import com.itextpdf.signatures.IOcspClient;
import com.itextpdf.signatures.IssuingCertificateRetriever;
import com.itextpdf.signatures.OID;
import com.itextpdf.signatures.OcspClientBouncyCastle;
import com.itextpdf.signatures.validation.v1.context.CertificateSource;
import com.itextpdf.signatures.validation.v1.context.TimeBasedContext;
import com.itextpdf.signatures.validation.v1.context.ValidationContext;
import com.itextpdf.signatures.validation.v1.context.ValidatorContext;
import com.itextpdf.signatures.validation.v1.report.CertificateReportItem;
import com.itextpdf.signatures.validation.v1.report.ReportItem;
import com.itextpdf.signatures.validation.v1.report.ReportItem.ReportItemStatus;
import com.itextpdf.signatures.validation.v1.report.ValidationReport;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Class that allows you to fetch and validate revocation data for the certificate.
 */
public class RevocationDataValidator {
    static final String REVOCATION_DATA_CHECK = "Revocation data check.";
    static final String NO_REVOCATION_DATA = "Certificate revocation status cannot be checked: " +
            "no revocation data available or the status cannot be determined.";
    static final String SELF_SIGNED_CERTIFICATE = "Certificate is self-signed. Revocation data check will be skipped.";
    static final String TRUSTED_OCSP_RESPONDER = "Authorized OCSP Responder certificate has id-pkix-ocsp-nocheck " +
            "extension so it is trusted by the definition and no revocation checking is performed.";
    static final String VALIDITY_ASSURED = "Certificate is trusted due to validity assured - short term extension.";
    static final String CANNOT_PARSE_OCSP =
            "OCSP response from \"{0}\" OCSP client cannot be parsed.";
    static final String CANNOT_PARSE_CRL =
            "CRL response from \"{0}\" CRL client cannot be parsed.";

    private static final IBouncyCastleFactory BOUNCY_CASTLE_FACTORY = BouncyCastleFactoryCreator.getFactory();

    private final List<IOcspClient> ocspClients = new ArrayList<>();
    private final List<ICrlClient> crlClients = new ArrayList<>();
    private final SignatureValidationProperties properties;
    private final IssuingCertificateRetriever certificateRetriever;
    private final OCSPValidator ocspValidator;
    private final CRLValidator crlValidator;

    /**
     * Creates new {@link RevocationDataValidator} instance to validate certificate revocation data.
     *
     * @param builder See {@link  ValidatorChainBuilder}
     */
    protected RevocationDataValidator(ValidatorChainBuilder builder) {
        this.certificateRetriever = builder.getCertificateRetriever();
        this.properties = builder.getProperties();
        this.ocspValidator = builder.getOCSPValidator();
        this.crlValidator = builder.getCRLValidator();
    }

    /**
     * Add {@link ICrlClient} to be used for CRL responses receiving.
     *
     * @param crlClient {@link ICrlClient} to be used for CRL responses receiving
     *
     * @return same instance of {@link RevocationDataValidator}.
     */
    public RevocationDataValidator addCrlClient(ICrlClient crlClient) {
        this.crlClients.add(crlClient);
        return this;
    }

    /**
     * Add {@link IOcspClient} to be used for OCSP responses receiving.
     *
     * @param ocspClient {@link IOcspClient} to be used for OCSP responses receiving
     *
     * @return same instance of {@link RevocationDataValidator}.
     */
    public RevocationDataValidator addOcspClient(IOcspClient ocspClient) {
        this.ocspClients.add(ocspClient);
        return this;
    }

    /**
     * Validates revocation data (Certificate Revocation List (CRL) Responses and OCSP Responses) of the certificate.
     *
     * @param report         to store all the verification results
     * @param context        {@link ValidationContext} the context
     * @param certificate    the certificate to check revocation data for
     * @param validationDate validation date to check for
     */
    public void validate(ValidationReport report, ValidationContext context, X509Certificate certificate,
            Date validationDate) {
        ValidationContext localContext = context.setValidatorContext(ValidatorContext.REVOCATION_DATA_VALIDATOR);
        if (CertificateUtil.isSelfSigned(certificate)) {
            report.addReportItem(new CertificateReportItem(certificate, REVOCATION_DATA_CHECK, SELF_SIGNED_CERTIFICATE,
                    ReportItemStatus.INFO));
            return;
        }
        // Check Validity Assured - Short Term extension which indicates that the validity of the certificate is assured
        // because the certificate is a "short-term certificate".
        if (CertificateUtil.getExtensionValueByOid(certificate,
                OID.X509Extensions.VALIDITY_ASSURED_SHORT_TERM) != null) {
            report.addReportItem(new CertificateReportItem(certificate, REVOCATION_DATA_CHECK, VALIDITY_ASSURED,
                    ReportItemStatus.INFO));
            return;
        }
        if (CertificateSource.OCSP_ISSUER == localContext.getCertificateSource()) {
            // Check if Authorised OCSP Responder certificate has id-pkix-ocsp-nocheck extension, in which case we
            // do not perform revocation check for it.
            if (CertificateUtil.getExtensionValueByOid(certificate, BOUNCY_CASTLE_FACTORY.createOCSPObjectIdentifiers()
                    .getIdPkixOcspNoCheck().getId()) != null) {
                report.addReportItem(new CertificateReportItem(certificate, REVOCATION_DATA_CHECK,
                        TRUSTED_OCSP_RESPONDER, ReportItemStatus.INFO));
                return;
            }
        }

        // Collect revocation data.
        List<OcspResponseValidationInfo> ocspResponses = retrieveAllOCSPResponses(report, localContext, certificate);
        ocspResponses = ocspResponses.stream().sorted((o1, o2) -> o2.singleResp.getThisUpdate().compareTo(
                o1.singleResp.getThisUpdate())).collect(Collectors.toList());
        List<CrlValidationInfo> crlResponses = retrieveAllCRLResponses(report, localContext, certificate);

        // Try to check responderCert for revocation using provided responder OCSP/CRL clients or
        // Authority Information Access for OCSP responses and CRL Distribution Points for CRL responses
        // using default clients.
        validateRevocationData(report, localContext, certificate, validationDate, ocspResponses, crlResponses);
    }

    private void validateRevocationData(ValidationReport report, ValidationContext context, X509Certificate certificate,
            Date validationDate, List<OcspResponseValidationInfo> ocspResponses, List<CrlValidationInfo> crlResponses) {
        int i = 0;
        int j = 0;
        while (i < ocspResponses.size() || j < crlResponses.size()) {
            ValidationReport revDataValidationReport = new ValidationReport();
            if (i < ocspResponses.size() && (j >= crlResponses.size() ||
                    ocspResponses.get(i).singleResp.getThisUpdate().after(crlResponses.get(j).crl.getThisUpdate()))) {
                OcspResponseValidationInfo validationInfo = ocspResponses.get(i);
                ocspValidator.validate(revDataValidationReport,
                        context.setTimeBasedContext(validationInfo.timeBasedContext), certificate,
                        validationInfo.singleResp, validationInfo.basicOCSPResp, validationDate,
                        validationInfo.trustedGenerationDate);
                i++;
            } else {
                CrlValidationInfo validationInfo = crlResponses.get(j);
                crlValidator.validate(revDataValidationReport,
                        context.setTimeBasedContext(validationInfo.timeBasedContext), certificate, validationInfo.crl,
                        validationDate, validationInfo.trustedGenerationDate);
                j++;
            }

            if (ValidationReport.ValidationResult.INDETERMINATE == revDataValidationReport.getValidationResult()) {
                for (ReportItem reportItem : revDataValidationReport.getLogs()) {
                    // These messages are useless for the user, we don't want them to be in the resulting report.
                    if (!OCSPValidator.SERIAL_NUMBERS_DO_NOT_MATCH.equals(reportItem.getMessage()) &&
                            !CRLValidator.CRL_ISSUER_NO_COMMON_ROOT.equals(reportItem.getMessage())) {
                        report.addReportItem(reportItem.setStatus(ReportItemStatus.INFO));
                    }
                }
            } else {
                report.merge(revDataValidationReport);
                return;
            }
        }

        report.addReportItem(new CertificateReportItem(certificate, REVOCATION_DATA_CHECK, NO_REVOCATION_DATA,
                ReportItemStatus.INDETERMINATE));
    }

    private List<OcspResponseValidationInfo> retrieveAllOCSPResponses(ValidationReport report,
            ValidationContext context, X509Certificate certificate) {
        List<OcspResponseValidationInfo> ocspResponses = new ArrayList<>();
        for (IOcspClient ocspClient : ocspClients) {
            if (ocspClient instanceof ValidationOcspClient) {
                ValidationOcspClient validationOcspClient = (ValidationOcspClient) ocspClient;
                for (Map.Entry<IBasicOCSPResp, OcspResponseValidationInfo> response :
                        validationOcspClient.getResponses().entrySet()) {
                    fillOcspResponses(ocspResponses, response.getKey(), response.getValue().trustedGenerationDate,
                            response.getValue().timeBasedContext);
                }
            } else {
                byte[] basicOcspRespBytes = ocspClient.getEncoded(certificate,
                        (X509Certificate) certificateRetriever.retrieveIssuerCertificate(certificate), null);
                if (basicOcspRespBytes != null) {
                    try {
                        IBasicOCSPResp basicOCSPResp = BOUNCY_CASTLE_FACTORY.createBasicOCSPResp(
                                BOUNCY_CASTLE_FACTORY.createBasicOCSPResponse(BOUNCY_CASTLE_FACTORY.createASN1Primitive(
                                        basicOcspRespBytes)));
                        fillOcspResponses(ocspResponses, basicOCSPResp, DateTimeUtil.getCurrentTimeDate(),
                                TimeBasedContext.PRESENT);
                    } catch (IOException e) {
                        report.addReportItem(new ReportItem(REVOCATION_DATA_CHECK, MessageFormatUtil.format(
                                CANNOT_PARSE_OCSP, ocspClient), e, ReportItemStatus.INFO));
                    }
                }
            }
        }
        SignatureValidationProperties.OnlineFetching onlineFetching = properties.getRevocationOnlineFetching(
                context.setValidatorContext(ValidatorContext.OCSP_VALIDATOR));
        if (SignatureValidationProperties.OnlineFetching.ALWAYS_FETCH == onlineFetching ||
                (SignatureValidationProperties.OnlineFetching.FETCH_IF_NO_OTHER_DATA_AVAILABLE == onlineFetching
                        && ocspResponses.isEmpty())) {
            IBasicOCSPResp basicOCSPResp = new OcspClientBouncyCastle(null).getBasicOCSPResp(certificate,
                    (X509Certificate) certificateRetriever.retrieveIssuerCertificate(certificate), null);
            fillOcspResponses(ocspResponses, basicOCSPResp, DateTimeUtil.getCurrentTimeDate(),
                    TimeBasedContext.PRESENT);
        }
        return ocspResponses;
    }

    private List<CrlValidationInfo> retrieveAllCRLResponses(ValidationReport report, ValidationContext context,
            X509Certificate certificate) {
        List<CrlValidationInfo> crlResponses = new ArrayList<>();
        for (ICrlClient crlClient : crlClients) {
            crlResponses.addAll(retrieveAllCRLResponsesUsingClient(report, certificate, crlClient));
        }
        SignatureValidationProperties.OnlineFetching onLineFetching = properties.getRevocationOnlineFetching(
                context.setValidatorContext(ValidatorContext.CRL_VALIDATOR));
        if (SignatureValidationProperties.OnlineFetching.ALWAYS_FETCH == onLineFetching ||
                (SignatureValidationProperties.OnlineFetching.FETCH_IF_NO_OTHER_DATA_AVAILABLE == onLineFetching &&
                        crlResponses.isEmpty())) {
            crlResponses.addAll(retrieveAllCRLResponsesUsingClient(report, certificate, new CrlClientOnline()));
        }
        // Sort all the CRL responses available based on the most recent revocation data.
        return crlResponses.stream().sorted((o1, o2) -> o2.crl.getThisUpdate().compareTo(o1.crl.getThisUpdate()))
                .collect(Collectors.toList());
    }

    private static void fillOcspResponses(List<OcspResponseValidationInfo> ocspResponses, IBasicOCSPResp basicOCSPResp,
            Date generationDate, TimeBasedContext timeBasedContext) {
        if (basicOCSPResp != null) {
            // Getting the responses.
            ISingleResp[] singleResponses = basicOCSPResp.getResponses();
            for (ISingleResp singleResponse : singleResponses) {
                ocspResponses.add(new OcspResponseValidationInfo(singleResponse, basicOCSPResp, generationDate,
                        timeBasedContext));
            }
        }
    }

    private static List<CrlValidationInfo> retrieveAllCRLResponsesUsingClient(ValidationReport report,
            X509Certificate certificate, ICrlClient crlClient) {
        List<CrlValidationInfo> crlResponses = new ArrayList<>();
        if (crlClient instanceof ValidationCrlClient) {
            ValidationCrlClient validationCrlClient = (ValidationCrlClient) crlClient;
            crlResponses.addAll(validationCrlClient.getCrls().values());
        } else {
            try {
                Collection<byte[]> crlBytesCollection = crlClient.getEncoded(certificate, null);
                for (byte[] crlBytes : crlBytesCollection) {
                    try {
                        crlResponses.add(new CrlValidationInfo((X509CRL) CertificateUtil.parseCrlFromBytes(crlBytes),
                                DateTimeUtil.getCurrentTimeDate(), TimeBasedContext.PRESENT));
                    } catch (Exception ignored) {
                        report.addReportItem(new CertificateReportItem(certificate, REVOCATION_DATA_CHECK,
                                MessageFormatUtil.format(CANNOT_PARSE_CRL, crlClient), ReportItemStatus.INFO));
                    }
                }
            } catch (GeneralSecurityException ignored) {
                report.addReportItem(new CertificateReportItem(certificate, REVOCATION_DATA_CHECK,
                        MessageFormatUtil.format(CANNOT_PARSE_CRL, crlClient), ReportItemStatus.INFO));
            }
        }
        return crlResponses;
    }

    /**
     * Class which contains validation related information about single OCSP response.
     */
    public static class OcspResponseValidationInfo {
        final ISingleResp singleResp;
        final IBasicOCSPResp basicOCSPResp;
        final Date trustedGenerationDate;
        final TimeBasedContext timeBasedContext;

        /**
         * Creates validation related information about single OCSP response.
         *
         * @param singleResp            {@link ISingleResp} single response to be validated
         * @param basicOCSPResp         {@link IBasicOCSPResp} basic OCSP response which contains this single response
         * @param trustedGenerationDate {@link Date} trusted date at which response was generated
         * @param timeBasedContext      {@link TimeBasedContext} time based context which corresponds to generation date
         */
        public OcspResponseValidationInfo(ISingleResp singleResp, IBasicOCSPResp basicOCSPResp,
                Date trustedGenerationDate, TimeBasedContext timeBasedContext) {
            this.singleResp = singleResp;
            this.basicOCSPResp = basicOCSPResp;
            this.trustedGenerationDate = trustedGenerationDate;
            this.timeBasedContext = timeBasedContext;
        }
    }

    /**
     * Class which contains validation related information about CRL response.
     */
    public static class CrlValidationInfo {
        final X509CRL crl;
        final Date trustedGenerationDate;
        final TimeBasedContext timeBasedContext;

        /**
         * Creates validation related information about CRL response.
         *
         * @param crl                   {@link X509CRL} CRL to be validated
         * @param trustedGenerationDate {@link Date} trusted date at which response was generated
         * @param timeBasedContext      {@link TimeBasedContext} time based context which corresponds to generation date
         */
        public CrlValidationInfo(X509CRL crl, Date trustedGenerationDate, TimeBasedContext timeBasedContext) {
            this.crl = crl;
            this.trustedGenerationDate = trustedGenerationDate;
            this.timeBasedContext = timeBasedContext;
        }
    }
}
