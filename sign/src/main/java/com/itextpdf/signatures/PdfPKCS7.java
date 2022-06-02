/*

    This file is part of the iText (R) project.
    Copyright (c) 1998-2022 iText Group NV
    Authors: Bruno Lowagie, Paulo Soares, et al.

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License version 3
    as published by the Free Software Foundation with the addition of the
    following permission added to Section 15 as permitted in Section 7(a):
    FOR ANY PART OF THE COVERED WORK IN WHICH THE COPYRIGHT IS OWNED BY
    ITEXT GROUP. ITEXT GROUP DISCLAIMS THE WARRANTY OF NON INFRINGEMENT
    OF THIRD PARTY RIGHTS

    This program is distributed in the hope that it will be useful, but
    WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
    or FITNESS FOR A PARTICULAR PURPOSE.
    See the GNU Affero General Public License for more details.
    You should have received a copy of the GNU Affero General Public License
    along with this program; if not, see http://www.gnu.org/licenses or write to
    the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor,
    Boston, MA, 02110-1301 USA, or download the license from the following URL:
    http://itextpdf.com/terms-of-use/

    The interactive user interfaces in modified source and object code versions
    of this program must display Appropriate Legal Notices, as required under
    Section 5 of the GNU Affero General Public License.

    In accordance with Section 7(b) of the GNU Affero General Public License,
    a covered work must retain the producer line in every PDF that is created
    or manipulated using iText.

    You can be released from the requirements of the license by purchasing
    a commercial license. Buying such a license is mandatory as soon as you
    develop commercial activities involving the iText software without
    disclosing the source code of your own applications.
    These activities include: offering paid services to customers as an ASP,
    serving PDFs on the fly in a web application, shipping iText with a closed
    source product.

    For more information, please contact iText Software Corp. at this
    address: sales@itextpdf.com
 */
package com.itextpdf.signatures;

import com.itextpdf.commons.bouncycastle.IBouncyCastleFactory;
import com.itextpdf.commons.bouncycastle.asn1.IASN1EncodableVector;
import com.itextpdf.commons.bouncycastle.asn1.IASN1Enumerated;
import com.itextpdf.commons.bouncycastle.asn1.IASN1InputStream;
import com.itextpdf.commons.bouncycastle.asn1.IASN1ObjectIdentifier;
import com.itextpdf.commons.bouncycastle.asn1.IASN1OctetString;
import com.itextpdf.commons.bouncycastle.asn1.IASN1OutputStream;
import com.itextpdf.commons.bouncycastle.asn1.IASN1Primitive;
import com.itextpdf.commons.bouncycastle.asn1.IASN1Sequence;
import com.itextpdf.commons.bouncycastle.asn1.IASN1Set;
import com.itextpdf.commons.bouncycastle.asn1.IASN1TaggedObject;
import com.itextpdf.commons.bouncycastle.asn1.IDEROctetString;
import com.itextpdf.commons.bouncycastle.asn1.IDERSequence;
import com.itextpdf.commons.bouncycastle.asn1.IDERSet;
import com.itextpdf.commons.bouncycastle.asn1.cms.IAttribute;
import com.itextpdf.commons.bouncycastle.asn1.cms.IAttributeTable;
import com.itextpdf.commons.bouncycastle.asn1.cms.IContentInfo;
import com.itextpdf.commons.bouncycastle.asn1.esf.ISignaturePolicyIdentifier;
import com.itextpdf.commons.bouncycastle.asn1.ess.IESSCertID;
import com.itextpdf.commons.bouncycastle.asn1.ess.IESSCertIDv2;
import com.itextpdf.commons.bouncycastle.asn1.ess.ISigningCertificate;
import com.itextpdf.commons.bouncycastle.asn1.ess.ISigningCertificateV2;
import com.itextpdf.commons.bouncycastle.asn1.ocsp.IBasicOCSPResponse;
import com.itextpdf.commons.bouncycastle.asn1.ocsp.IOCSPObjectIdentifiers;
import com.itextpdf.commons.bouncycastle.asn1.pkcs.IPKCSObjectIdentifiers;
import com.itextpdf.commons.bouncycastle.asn1.tsp.IMessageImprint;
import com.itextpdf.commons.bouncycastle.asn1.x509.IAlgorithmIdentifier;
import com.itextpdf.commons.bouncycastle.cert.ocsp.IBasicOCSPResp;
import com.itextpdf.commons.bouncycastle.cert.ocsp.ICertificateID;
import com.itextpdf.commons.bouncycastle.cert.ocsp.ISingleResp;
import com.itextpdf.commons.bouncycastle.jce.IX509Principal;
import com.itextpdf.commons.bouncycastle.tsp.ITimeStampToken;
import com.itextpdf.commons.bouncycastle.tsp.ITimeStampTokenInfo;
import com.itextpdf.kernel.bouncycastle.BouncyCastleFactoryCreator;
import com.itextpdf.kernel.exceptions.PdfException;
import com.itextpdf.kernel.pdf.PdfName;
import com.itextpdf.signatures.exceptions.SignExceptionMessageConstant;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.CRL;
import java.security.cert.Certificate;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This class does all the processing related to signing
 * and verifying a PKCS#7 signature.
 */
public class PdfPKCS7 {

    private static final IBouncyCastleFactory bouncyCastleFactory = BouncyCastleFactoryCreator.getFactory();

    private ISignaturePolicyIdentifier signaturePolicyIdentifier;

    // Encryption provider

    /**
     * The encryption provider, e.g. "BC" if you use BouncyCastle.
     */
    private String provider;

    // Signature info

    /**
     * Holds value of property signName.
     */
    private String signName;

    /**
     * Holds value of property reason.
     */
    private String reason;

    /**
     * Holds value of property location.
     */
    private String location;

    /**
     * Holds value of property signDate.
     */
    private Calendar signDate = (Calendar) TimestampConstants.UNDEFINED_TIMESTAMP_DATE;

    // Constructors for creating new signatures

    /**
     * Assembles all the elements needed to create a signature, except for the data.
     *
     * @param privKey         the private key
     * @param certChain       the certificate chain
     * @param interfaceDigest the interface digest
     * @param hashAlgorithm   the hash algorithm
     * @param provider        the provider or <code>null</code> for the default provider
     * @param hasRSAdata      <CODE>true</CODE> if the sub-filter is adbe.pkcs7.sha1
     * @throws InvalidKeyException      on error
     * @throws NoSuchProviderException  on error
     * @throws NoSuchAlgorithmException on error
     */
    public PdfPKCS7(PrivateKey privKey, Certificate[] certChain,
                    String hashAlgorithm, String provider, IExternalDigest interfaceDigest, boolean hasRSAdata)
            throws InvalidKeyException, NoSuchProviderException, NoSuchAlgorithmException {
        this.provider = provider;
        this.interfaceDigest = interfaceDigest;
        // message digest
        digestAlgorithmOid = DigestAlgorithms.getAllowedDigest(hashAlgorithm);
        if (digestAlgorithmOid == null) {
            throw new PdfException(SignExceptionMessageConstant.UNKNOWN_HASH_ALGORITHM)
                    .setMessageParams(hashAlgorithm);
        }

        // Copy the certificates
        signCert = (X509Certificate) certChain[0];
        certs = new ArrayList<>();
        for (Certificate element : certChain) {
            certs.add(element);
        }

        // initialize and add the digest algorithms.
        digestalgos = new HashSet<>();
        digestalgos.add(digestAlgorithmOid);

        // find the signing algorithm (RSA or DSA)
        if (privKey != null) {
            digestEncryptionAlgorithmOid = SignUtils.getPrivateKeyAlgorithm(privKey);
            if (digestEncryptionAlgorithmOid.equals("RSA")) {
                digestEncryptionAlgorithmOid = SecurityIDs.ID_RSA;
            } else if (digestEncryptionAlgorithmOid.equals("DSA")) {
                digestEncryptionAlgorithmOid = SecurityIDs.ID_DSA;
            } else {
                throw new PdfException(
                        SignExceptionMessageConstant.UNKNOWN_KEY_ALGORITHM).setMessageParams(digestEncryptionAlgorithmOid);
            }
        }

        // initialize the RSA data
        if (hasRSAdata) {
            rsaData = new byte[0];
            messageDigest = DigestAlgorithms.getMessageDigest(getHashAlgorithm(), provider);
        }

        // initialize the Signature object
        if (privKey != null) {
            sig = initSignature(privKey);
        }
    }

    // Constructors for validating existing signatures

    /**
     * Use this constructor if you want to verify a signature using the sub-filter adbe.x509.rsa_sha1.
     *
     * @param contentsKey the /Contents key
     * @param certsKey    the /Cert key
     * @param provider    the provider or <code>null</code> for the default provider
     */
    @SuppressWarnings("unchecked")
    public PdfPKCS7(byte[] contentsKey, byte[] certsKey, String provider) {
        try {
            this.provider = provider;
            certs = SignUtils.readAllCerts(certsKey);
            signCerts = certs;
            signCert = (X509Certificate) SignUtils.getFirstElement(certs);
            crls = new ArrayList<>();

            IASN1InputStream in = bouncyCastleFactory.createInputStream(new ByteArrayInputStream(contentsKey));
            digest = bouncyCastleFactory.createOctetString(in.readObject()).getOctets();

            sig = SignUtils.getSignatureHelper("SHA1withRSA", provider);
            sig.initVerify(signCert.getPublicKey());

            // setting the oid to SHA1withRSA
            digestAlgorithmOid = "1.2.840.10040.4.3";
            digestEncryptionAlgorithmOid = "1.3.36.3.3.1.2";
        } catch (Exception e) {
            throw new PdfException(e);
        }
    }

    /**
     * Use this constructor if you want to verify a signature.
     *
     * @param contentsKey   the /Contents key
     * @param filterSubtype the filtersubtype
     * @param provider      the provider or <code>null</code> for the default provider
     */
    @SuppressWarnings({"unchecked"})
    public PdfPKCS7(byte[] contentsKey, PdfName filterSubtype, String provider) {
        this.filterSubtype = filterSubtype;
        isTsp = PdfName.ETSI_RFC3161.equals(filterSubtype);
        isCades = PdfName.ETSI_CAdES_DETACHED.equals(filterSubtype);
        try {
            this.provider = provider;
            IASN1InputStream din = bouncyCastleFactory.createInputStream(new ByteArrayInputStream(contentsKey));

            //
            // Basic checks to make sure it's a PKCS#7 SignedData Object
            //
            IASN1Primitive pkcs;

            try {
                pkcs = din.readObject();
            } catch (IOException e) {
                throw new IllegalArgumentException(
                        SignExceptionMessageConstant.CANNOT_DECODE_PKCS7_SIGNED_DATA_OBJECT);
            }
            IASN1Sequence signedData = bouncyCastleFactory.createSequence(pkcs);
            if (signedData == null) {
                throw new IllegalArgumentException(
                        SignExceptionMessageConstant.NOT_A_VALID_PKCS7_OBJECT_NOT_A_SEQUENCE);
            }
            IASN1ObjectIdentifier objId = bouncyCastleFactory.createObjectIdentifier(signedData.getObjectAt(0));
            if (!objId.getId().equals(SecurityIDs.ID_PKCS7_SIGNED_DATA))
                throw new IllegalArgumentException(
                        SignExceptionMessageConstant.NOT_A_VALID_PKCS7_OBJECT_NOT_SIGNED_DATA);
            IASN1Sequence content = bouncyCastleFactory.createSequence(
                    bouncyCastleFactory.createTaggedObject(signedData.getObjectAt(1)).getObject());
            // the positions that we care are:
            //     0 - version
            //     1 - digestAlgorithms
            //     2 - possible ID_PKCS7_DATA
            //     (the certificates and crls are taken out by other means)
            //     last - signerInfos

            // the version
            version = bouncyCastleFactory.createInteger(content.getObjectAt(0)).getValue().intValue();

            // the digestAlgorithms
            digestalgos = new HashSet<>();
            Enumeration e = bouncyCastleFactory.createSet(content.getObjectAt(1)).getObjects();
            while (e.hasMoreElements()) {
                IASN1Sequence s = bouncyCastleFactory.createSequence(e.nextElement());
                IASN1ObjectIdentifier o = bouncyCastleFactory.createObjectIdentifier(s.getObjectAt(0));
                digestalgos.add(o.getId());
            }

            // the possible ID_PKCS7_DATA
            IASN1Sequence rsaData = bouncyCastleFactory.createSequence(content.getObjectAt(2));
            if (rsaData.size() > 1) {
                IASN1OctetString rsaDataContent = bouncyCastleFactory.createOctetString(
                        bouncyCastleFactory.createTaggedObject(rsaData.getObjectAt(1)).getObject());
                this.rsaData = rsaDataContent.getOctets();
            }

            int next = 3;
            while (bouncyCastleFactory.createTaggedObject(content.getObjectAt(next)) != null) {
                ++next;
            }


            // the certificates
/*
            This should work, but that's not always the case because of a bug in BouncyCastle:
*/
            certs = SignUtils.readAllCerts(contentsKey);

            // the signerInfos
            IASN1Set signerInfos = bouncyCastleFactory.createSet(content.getObjectAt(next));
            if (signerInfos.size() != 1)
                throw new IllegalArgumentException(
                        SignExceptionMessageConstant.THIS_PKCS7_OBJECT_HAS_MULTIPLE_SIGNERINFOS_ONLY_ONE_IS_SUPPORTED_AT_THIS_TIME);
            IASN1Sequence signerInfo = bouncyCastleFactory.createSequence(signerInfos.getObjectAt(0));
            // the positions that we care are
            //     0 - version
            //     1 - the signing certificate issuer and serial number
            //     2 - the digest algorithm
            //     3 or 4 - digestEncryptionAlgorithm
            //     4 or 5 - encryptedDigest
            signerversion = bouncyCastleFactory.createInteger(signerInfo.getObjectAt(0)).getValue().intValue();
            // Get the signing certificate
            IASN1Sequence issuerAndSerialNumber = bouncyCastleFactory.createSequence(signerInfo.getObjectAt(1));
            IX509Principal issuer = SignUtils.getIssuerX509Name(issuerAndSerialNumber);
            BigInteger serialNumber = bouncyCastleFactory.createInteger(issuerAndSerialNumber.getObjectAt(1)).getValue();
            for (Object element : certs) {
                X509Certificate cert = (X509Certificate) element;
                if (cert.getIssuerDN().equals(issuer) && serialNumber.equals(cert.getSerialNumber())) {
                    signCert = cert;
                    break;
                }
            }
            if (signCert == null) {
                throw new PdfException(SignExceptionMessageConstant.CANNOT_FIND_SIGNING_CERTIFICATE_WITH_THIS_SERIAL).
                        setMessageParams(issuer.getName() + " / " + serialNumber.toString(16));
            }
            signCertificateChain();
            digestAlgorithmOid = bouncyCastleFactory.createObjectIdentifier(
                    bouncyCastleFactory.createSequence(signerInfo.getObjectAt(2)).getObjectAt(0)).getId();
            next = 3;
            boolean foundCades = false;
            IASN1TaggedObject tagsig = bouncyCastleFactory.createTaggedObject(signerInfo.getObjectAt(next));
            if (tagsig != null) {
                IASN1Set sseq = bouncyCastleFactory.createSetInstance(tagsig, false);
                sigAttr = sseq.getEncoded();
                // maybe not necessary, but we use the following line as fallback:
                sigAttrDer = sseq.getEncoded(bouncyCastleFactory.createEncoding().getDer());

                for (int k = 0; k < sseq.size(); ++k) {
                    IASN1Sequence seq2 = bouncyCastleFactory.createSequence(sseq.getObjectAt(k));
                    String idSeq2 = bouncyCastleFactory.createObjectIdentifier(seq2.getObjectAt(0)).getId();
                    if (idSeq2.equals(SecurityIDs.ID_MESSAGE_DIGEST)) {
                        IASN1Set set = bouncyCastleFactory.createSet(seq2.getObjectAt(1));
                        digestAttr = bouncyCastleFactory.createOctetString(set.getObjectAt(0)).getOctets();
                    } else if (idSeq2.equals(SecurityIDs.ID_ADBE_REVOCATION)) {
                        IASN1Set setout = bouncyCastleFactory.createSet(seq2.getObjectAt(1));
                        IASN1Sequence seqout = bouncyCastleFactory.createSequence(setout.getObjectAt(0));
                        for (int j = 0; j < seqout.size(); ++j) {
                            IASN1TaggedObject tg = bouncyCastleFactory.createTaggedObject(seqout.getObjectAt(j));
                            if (tg.getTagNo() == 0) {
                                IASN1Sequence seqin = bouncyCastleFactory.createSequence(tg.getObject());
                                findCRL(seqin);
                            }
                            if (tg.getTagNo() == 1) {
                                IASN1Sequence seqin = bouncyCastleFactory.createSequence(tg.getObject());
                                findOcsp(seqin);
                            }
                        }
                    } else if (isCades && idSeq2.equals(SecurityIDs.ID_AA_SIGNING_CERTIFICATE_V1)) {
                        IASN1Set setout = bouncyCastleFactory.createSet(seq2.getObjectAt(1));
                        IASN1Sequence seqout = bouncyCastleFactory.createSequence(setout.getObjectAt(0));
                        ISigningCertificate sv2 = bouncyCastleFactory.createSigningCertificate(seqout);
                        IESSCertID[] cerv2m = sv2.getCerts();
                        IESSCertID cerv2 = cerv2m[0];
                        byte[] enc2 = signCert.getEncoded();
                        MessageDigest m2 = SignUtils.getMessageDigest("SHA-1");
                        byte[] signCertHash = m2.digest(enc2);
                        byte[] hs2 = cerv2.getCertHash();
                        if (!Arrays.equals(signCertHash, hs2))
                            throw new IllegalArgumentException("Signing certificate doesn't match the ESS information.");
                        foundCades = true;
                    } else if (isCades && idSeq2.equals(SecurityIDs.ID_AA_SIGNING_CERTIFICATE_V2)) {
                        IASN1Set setout = bouncyCastleFactory.createSet(seq2.getObjectAt(1));
                        IASN1Sequence seqout = bouncyCastleFactory.createSequence(setout.getObjectAt(0));
                        ISigningCertificateV2 sv2 = bouncyCastleFactory.createSigningCertificateV2(seqout);
                        IESSCertIDv2[] cerv2m = sv2.getCerts();
                        IESSCertIDv2 cerv2 = cerv2m[0];
                        IAlgorithmIdentifier ai2 = cerv2.getHashAlgorithm();
                        byte[] enc2 = signCert.getEncoded();
                        MessageDigest m2
                                = SignUtils.getMessageDigest(DigestAlgorithms.getDigest(ai2.getAlgorithm().getId()));
                        byte[] signCertHash = m2.digest(enc2);
                        byte[] hs2 = cerv2.getCertHash();
                        if (!Arrays.equals(signCertHash, hs2))
                            throw new IllegalArgumentException("Signing certificate doesn't match the ESS information.");
                        foundCades = true;
                    }
                }
                if (digestAttr == null)
                    throw new IllegalArgumentException(
                            SignExceptionMessageConstant.AUTHENTICATED_ATTRIBUTE_IS_MISSING_THE_DIGEST);
                ++next;
            }
            if (isCades && !foundCades)
                throw new IllegalArgumentException("CAdES ESS information missing.");
            digestEncryptionAlgorithmOid = bouncyCastleFactory.createObjectIdentifier(
                    bouncyCastleFactory.createSequence(signerInfo.getObjectAt(next++)).getObjectAt(0)).getId();
            digest = bouncyCastleFactory.createOctetString(signerInfo.getObjectAt(next++)).getOctets();
            if (next < signerInfo.size()) {
                IASN1TaggedObject taggedObject = bouncyCastleFactory.createTaggedObject(signerInfo.getObjectAt(next));
                if (taggedObject != null) {
                    IASN1Set unat = bouncyCastleFactory.createSetInstance(taggedObject, false);
                    IAttributeTable attble = bouncyCastleFactory.createAttributeTable(unat);
                    IPKCSObjectIdentifiers ipkcsObjectIdentifiers = bouncyCastleFactory.createPKCSObjectIdentifiers();
                    IAttribute ts = attble.get(ipkcsObjectIdentifiers.getIdAaSignatureTimeStampToken());
                    if (ts != null && ts.getAttrValues().size() > 0) {
                        IASN1Set attributeValues = ts.getAttrValues();
                        IASN1Sequence tokenSequence =
                                bouncyCastleFactory.createSequenceInstance(attributeValues.getObjectAt(0));
                        IContentInfo contentInfo = bouncyCastleFactory.createContentInfo(tokenSequence);
                        this.timeStampToken = bouncyCastleFactory.createTimeStampToken(contentInfo);
                    }
                }
            }
            if (isTsp) {
                IContentInfo contentInfoTsp = bouncyCastleFactory.createContentInfo(signedData);
                this.timeStampToken = bouncyCastleFactory.createTimeStampToken(contentInfoTsp);
                ITimeStampTokenInfo info = timeStampToken.getTimeStampInfo();
                String algOID = info.getHashAlgorithm().getAlgorithm().getId();
                messageDigest = DigestAlgorithms.getMessageDigestFromOid(algOID, null);
            } else {
                if (this.rsaData != null || digestAttr != null) {
                    if (PdfName.Adbe_pkcs7_sha1.equals(getFilterSubtype())) {
                        messageDigest = DigestAlgorithms.getMessageDigest("SHA1", provider);
                    } else {
                        messageDigest = DigestAlgorithms.getMessageDigest(getHashAlgorithm(), provider);
                    }
                    encContDigest = DigestAlgorithms.getMessageDigest(getHashAlgorithm(), provider);
                }
                sig = initSignature(signCert.getPublicKey());
            }
        } catch (Exception e) {
            throw new PdfException(e);
        }
    }

    public void setSignaturePolicy(SignaturePolicyInfo signaturePolicy) {
        this.signaturePolicyIdentifier = signaturePolicy.toSignaturePolicyIdentifier();
    }

    public void setSignaturePolicy(ISignaturePolicyIdentifier signaturePolicy) {
        this.signaturePolicyIdentifier = signaturePolicy;
    }

    /**
     * Getter for property sigName.
     *
     * @return Value of property sigName.
     */
    public String getSignName() {
        return this.signName;
    }

    /**
     * Setter for property sigName.
     *
     * @param signName New value of property sigName.
     */
    public void setSignName(String signName) {
        this.signName = signName;
    }

    /**
     * Getter for property reason.
     *
     * @return Value of property reason.
     */
    public String getReason() {
        return this.reason;
    }

    /**
     * Setter for property reason.
     *
     * @param reason New value of property reason.
     */
    public void setReason(String reason) {
        this.reason = reason;
    }

    /**
     * Getter for property location.
     *
     * @return Value of property location.
     */
    public String getLocation() {
        return this.location;
    }

    /**
     * Setter for property location.
     *
     * @param location New value of property location.
     */
    public void setLocation(String location) {
        this.location = location;
    }

    /**
     * Getter for property signDate.
     *
     * @return Value of property signDate.
     */
    public Calendar getSignDate() {
        Calendar dt = getTimeStampDate();
        if (dt == TimestampConstants.UNDEFINED_TIMESTAMP_DATE) {
            return this.signDate;
        } else {
            return dt;
        }
    }

    /**
     * Setter for property signDate.
     *
     * @param signDate New value of property signDate.
     */
    public void setSignDate(Calendar signDate) {
        this.signDate = signDate;
    }

    // version info

    /**
     * Version of the PKCS#7 object
     */
    private int version = 1;

    /**
     * Version of the PKCS#7 "SignerInfo" object.
     */
    private int signerversion = 1;

    /**
     * Get the version of the PKCS#7 object.
     *
     * @return the version of the PKCS#7 object.
     */
    public int getVersion() {
        return version;
    }

    /**
     * Get the version of the PKCS#7 "SignerInfo" object.
     *
     * @return the version of the PKCS#7 "SignerInfo" object.
     */
    public int getSigningInfoVersion() {
        return signerversion;
    }

    // Message digest algorithm

    /**
     * The ID of the digest algorithm, e.g. "2.16.840.1.101.3.4.2.1".
     */
    private String digestAlgorithmOid;

    /**
     * The object that will create the digest
     */
    private MessageDigest messageDigest;

    /**
     * The digest algorithms
     */
    private Set<String> digestalgos;

    /**
     * The digest attributes
     */
    private byte[] digestAttr;

    private PdfName filterSubtype;

    /**
     * Getter for the ID of the digest algorithm, e.g. "2.16.840.1.101.3.4.2.1".
     * See ISO-32000-1, section 12.8.3.3 PKCS#7 Signatures as used in ISO 32000
     *
     * @return the ID of the digest algorithm
     */
    public String getDigestAlgorithmOid() {
        return digestAlgorithmOid;
    }

    /**
     * Returns the name of the digest algorithm, e.g. "SHA256".
     *
     * @return the digest algorithm name, e.g. "SHA256"
     */
    public String getHashAlgorithm() {
        return DigestAlgorithms.getDigest(digestAlgorithmOid);
    }

    // Encryption algorithm

    /**
     * The encryption algorithm.
     */
    private String digestEncryptionAlgorithmOid;

    /**
     * Getter for the digest encryption algorithm.
     * See ISO-32000-1, section 12.8.3.3 PKCS#7 Signatures as used in ISO 32000
     *
     * @return the encryption algorithm
     */
    public String getDigestEncryptionAlgorithmOid() {
        return digestEncryptionAlgorithmOid;
    }

    /**
     * Get the algorithm used to calculate the message digest, e.g. "SHA1withRSA".
     * See ISO-32000-1, section 12.8.3.3 PKCS#7 Signatures as used in ISO 32000
     *
     * @return the algorithm used to calculate the message digest
     */
    public String getDigestAlgorithm() {
        return getHashAlgorithm() + "with" + getEncryptionAlgorithm();
    }

    /*
     *	DIGITAL SIGNATURE CREATION
     */

    private IExternalDigest interfaceDigest;
    // The signature is created externally

    /**
     * The signed digest if created outside this class
     */
    private byte externalDigest[];

    /**
     * External RSA data
     */
    private byte externalRsaData[];


    /**
     * Sets the digest/signature to an external calculated value.
     *
     * @param digest                    the digest. This is the actual signature
     * @param rsaData                   the extra data that goes into the data tag in PKCS#7
     * @param digestEncryptionAlgorithm the encryption algorithm. It may must be <CODE>null</CODE> if the <CODE>digest</CODE>
     *                                  is also <CODE>null</CODE>. If the <CODE>digest</CODE> is not <CODE>null</CODE>
     *                                  then it may be "RSA" or "DSA"
     */
    public void setExternalDigest(byte[] digest, byte[] rsaData, String digestEncryptionAlgorithm) {
        externalDigest = digest;
        externalRsaData = rsaData;
        if (digestEncryptionAlgorithm != null) {
            if (digestEncryptionAlgorithm.equals("RSA")) {
                this.digestEncryptionAlgorithmOid = SecurityIDs.ID_RSA;
            } else if (digestEncryptionAlgorithm.equals("DSA")) {
                this.digestEncryptionAlgorithmOid = SecurityIDs.ID_DSA;
            } else if (digestEncryptionAlgorithm.equals("ECDSA")) {
                this.digestEncryptionAlgorithmOid = SecurityIDs.ID_ECDSA;
            } else {
                throw new PdfException(SignExceptionMessageConstant.UNKNOWN_KEY_ALGORITHM)
                        .setMessageParams(digestEncryptionAlgorithm);
            }
        }
    }

    // The signature is created internally

    /**
     * Class from the Java SDK that provides the functionality of a digital signature algorithm.
     */
    private Signature sig;

    /**
     * The signed digest as calculated by this class (or extracted from an existing PDF)
     */
    private byte[] digest;

    /**
     * The RSA data
     */
    private byte[] rsaData;

    // Signing functionality.

    private Signature initSignature(PrivateKey key) throws NoSuchAlgorithmException, NoSuchProviderException,
            InvalidKeyException {
        Signature signature = SignUtils.getSignatureHelper(getDigestAlgorithm(), provider);
        signature.initSign(key);
        return signature;
    }

    private Signature initSignature(PublicKey key) throws NoSuchAlgorithmException, NoSuchProviderException,
            InvalidKeyException {
        String digestAlgorithm = getDigestAlgorithm();
        if (PdfName.Adbe_x509_rsa_sha1.equals(getFilterSubtype()))
            digestAlgorithm = "SHA1withRSA";
        Signature signature = SignUtils.getSignatureHelper(digestAlgorithm, provider);
        signature.initVerify(key);
        return signature;
    }

    /**
     * Update the digest with the specified bytes.
     * This method is used both for signing and verifying
     *
     * @param buf the data buffer
     * @param off the offset in the data buffer
     * @param len the data length
     * @throws SignatureException on error
     */
    public void update(byte[] buf, int off, int len) throws SignatureException {
        if (rsaData != null || digestAttr != null || isTsp) {
            messageDigest.update(buf, off, len);
        } else {
            sig.update(buf, off, len);
        }
    }

    // adbe.x509.rsa_sha1 (PKCS#1)

    /**
     * Gets the bytes for the PKCS#1 object.
     *
     * @return a byte array
     */
    public byte[] getEncodedPKCS1() {
        try {
            if (externalDigest != null)
                digest = externalDigest;
            else
                digest = sig.sign();
            ByteArrayOutputStream bOut = new ByteArrayOutputStream();

            IASN1OutputStream dout = bouncyCastleFactory.createOutputStream(bOut);
            dout.writeObject(bouncyCastleFactory.createDEROctetString(digest));
            dout.close();

            return bOut.toByteArray();
        } catch (Exception e) {
            throw new PdfException(e);
        }
    }

    // other subfilters (PKCS#7)

    /**
     * Gets the bytes for the PKCS7SignedData object.
     *
     * @return the bytes for the PKCS7SignedData object
     */
    public byte[] getEncodedPKCS7() {
        return getEncodedPKCS7(null, PdfSigner.CryptoStandard.CMS, null, null, null);
    }

    /**
     * Gets the bytes for the PKCS7SignedData object. Optionally the authenticatedAttributes
     * in the signerInfo can also be set. If either of the parameters is <CODE>null</CODE>, none will be used.
     *
     * @param secondDigest the digest in the authenticatedAttributes
     * @return the bytes for the PKCS7SignedData object
     */
    public byte[] getEncodedPKCS7(byte[] secondDigest) {
        return getEncodedPKCS7(secondDigest, PdfSigner.CryptoStandard.CMS, null, null, null);
    }

    /**
     * Gets the bytes for the PKCS7SignedData object. Optionally the authenticatedAttributes
     * in the signerInfo can also be set, and/or a time-stamp-authority client
     * may be provided.
     *
     * @param secondDigest the digest in the authenticatedAttributes
     * @param sigtype specifies the PKCS7 standard flavor to which created PKCS7SignedData object will adhere: either basic CMS or CAdES
     * @param tsaClient    TSAClient - null or an optional time stamp authority client
     * @param ocsp collection of DER-encoded BasicOCSPResponses for the  certificate in the signature certificates
     *             chain, or null if OCSP revocation data is not to be added.
     * @param crlBytes collection of DER-encoded CRL for certificates from the signature certificates chain,
     *                 or null if CRL revocation data is not to be added.
     * @see <a href="https://datatracker.ietf.org/doc/html/rfc6960#section-4.2.1">RFC 6960 § 4.2.1</a>
     * @return byte[] the bytes for the PKCS7SignedData object
     */
    public byte[] getEncodedPKCS7(byte[] secondDigest, PdfSigner.CryptoStandard sigtype, ITSAClient tsaClient, Collection<byte[]> ocsp, Collection<byte[]> crlBytes) {
        try {
            if (externalDigest != null) {
                digest = externalDigest;
                if (rsaData != null)
                    rsaData = externalRsaData;
            } else if (externalRsaData != null && rsaData != null) {
                rsaData = externalRsaData;
                sig.update(rsaData);
                digest = sig.sign();
            } else {
                if (rsaData != null) {
                    rsaData = messageDigest.digest();
                    sig.update(rsaData);
                }
                digest = sig.sign();
            }

            // Create the set of Hash algorithms
            IASN1EncodableVector digestAlgorithms = bouncyCastleFactory.createEncodableVector();
            for (Object element : digestalgos) {
                IASN1EncodableVector algos = bouncyCastleFactory.createEncodableVector();
                algos.add(bouncyCastleFactory.createObjectIdentifier((String) element));
                algos.add(bouncyCastleFactory.createDERNull());
                digestAlgorithms.add(bouncyCastleFactory.createDERSequence(algos));
            }

            // Create the contentInfo.
            IASN1EncodableVector v = bouncyCastleFactory.createEncodableVector();
            v.add(bouncyCastleFactory.createObjectIdentifier(SecurityIDs.ID_PKCS7_DATA));
            if (rsaData != null) {
                v.add(bouncyCastleFactory.createDERTaggedObject(0, bouncyCastleFactory.createDEROctetString(rsaData)));
            }
            IDERSequence contentinfo = bouncyCastleFactory.createDERSequence(v);

            // Get all the certificates
            //
            v = bouncyCastleFactory.createEncodableVector();
            for (Object element : certs) {
                IASN1InputStream tempstream = bouncyCastleFactory.createInputStream(
                        new ByteArrayInputStream(((X509Certificate) element).getEncoded()));
                v.add(tempstream.readObject());
            }

            IDERSet dercertificates = bouncyCastleFactory.createDERSet(v);

            // Create signerinfo structure.
            IASN1EncodableVector signerinfo = bouncyCastleFactory.createEncodableVector();

            // Add the signerInfo version
            signerinfo.add(bouncyCastleFactory.createInteger(signerversion));

            v = bouncyCastleFactory.createEncodableVector();

            v.add(CertificateInfo.getIssuer(signCert.getTBSCertificate()));
            v.add(bouncyCastleFactory.createInteger(signCert.getSerialNumber()));
            signerinfo.add(bouncyCastleFactory.createDERSequence(v));

            // Add the digestAlgorithm
            v = bouncyCastleFactory.createEncodableVector();
            v.add(bouncyCastleFactory.createObjectIdentifier(digestAlgorithmOid));
            v.add(bouncyCastleFactory.createDERNull());
            signerinfo.add(bouncyCastleFactory.createDERSequence(v));

            // add the authenticated attribute if present
            if (secondDigest != null) {
                signerinfo.add(bouncyCastleFactory.createDERTaggedObject(false, 0,
                        getAuthenticatedAttributeSet(secondDigest, ocsp, crlBytes, sigtype)));
            }
            // Add the digestEncryptionAlgorithm
            v = bouncyCastleFactory.createEncodableVector();
            v.add(bouncyCastleFactory.createObjectIdentifier(digestEncryptionAlgorithmOid));
            v.add(bouncyCastleFactory.createDERNull());
            signerinfo.add(bouncyCastleFactory.createDERSequence(v));

            // Add the digest
            signerinfo.add(bouncyCastleFactory.createDEROctetString(digest));

            // When requested, go get and add the timestamp. May throw an exception.
            // Added by Martin Brunecky, 07/12/2007 folowing Aiken Sam, 2006-11-15
            // Sam found Adobe expects time-stamped SHA1-1 of the encrypted digest
            if (tsaClient != null) {
                byte[] tsImprint = tsaClient.getMessageDigest().digest(digest);
                byte[] tsToken = tsaClient.getTimeStampToken(tsImprint);
                if (tsToken != null) {
                    IASN1EncodableVector unauthAttributes = buildUnauthenticatedAttributes(tsToken);
                    if (unauthAttributes != null) {
                        signerinfo.add(bouncyCastleFactory.createDERTaggedObject(
                                false, 1, bouncyCastleFactory.createDERSet(unauthAttributes)));
                    }
                }
            }

            // Finally build the body out of all the components above
            IASN1EncodableVector body = bouncyCastleFactory.createEncodableVector();
            body.add(bouncyCastleFactory.createInteger(version));
            body.add(bouncyCastleFactory.createDERSet(digestAlgorithms));
            body.add(contentinfo);
            body.add(bouncyCastleFactory.createDERTaggedObject(false, 0, dercertificates));

            // Only allow one signerInfo
            body.add(bouncyCastleFactory.createDERSet(bouncyCastleFactory.createDERSequence(signerinfo)));

            // Now we have the body, wrap it in it's PKCS7Signed shell
            // and return it
            //
            IASN1EncodableVector whole = bouncyCastleFactory.createEncodableVector();
            whole.add(bouncyCastleFactory.createObjectIdentifier(SecurityIDs.ID_PKCS7_SIGNED_DATA));
            whole.add(bouncyCastleFactory.createDERTaggedObject(0, bouncyCastleFactory.createDERSequence(body)));

            ByteArrayOutputStream bOut = new ByteArrayOutputStream();

            IASN1OutputStream dout = bouncyCastleFactory.createOutputStream(bOut);
            dout.writeObject(bouncyCastleFactory.createDERSequence(whole));
            dout.close();

            return bOut.toByteArray();
        } catch (Exception e) {
            throw new PdfException(e);
        }
    }

    /**
     * Added by Aiken Sam, 2006-11-15, modifed by Martin Brunecky 07/12/2007
     * to start with the timeStampToken (signedData 1.2.840.113549.1.7.2).
     * Token is the TSA response without response status, which is usually
     * handled by the (vendor supplied) TSA request/response interface).
     *
     * @param timeStampToken byte[] - time stamp token, DER encoded signedData
     * @return {@link IASN1EncodableVector}
     * @throws IOException
     */
    private IASN1EncodableVector buildUnauthenticatedAttributes(byte[] timeStampToken) throws IOException {
        if (timeStampToken == null)
            return null;

        // @todo: move this together with the rest of the defintions
        String ID_TIME_STAMP_TOKEN = "1.2.840.113549.1.9.16.2.14"; // RFC 3161 id-aa-timeStampToken

        IASN1InputStream tempstream = bouncyCastleFactory.createInputStream(new ByteArrayInputStream(timeStampToken));
        IASN1EncodableVector unauthAttributes = bouncyCastleFactory.createEncodableVector();

        IASN1EncodableVector v = bouncyCastleFactory.createEncodableVector();
        v.add(bouncyCastleFactory.createObjectIdentifier(ID_TIME_STAMP_TOKEN)); // id-aa-timeStampToken
        IASN1Sequence seq = bouncyCastleFactory.createSequence(tempstream.readObject());
        v.add(bouncyCastleFactory.createDERSet(seq));

        unauthAttributes.add(bouncyCastleFactory.createDERSequence(v));
        return unauthAttributes;
    }

    // Authenticated attributes

    /**
     * When using authenticatedAttributes the authentication process is different.
     * The document digest is generated and put inside the attribute. The signing is done over the DER encoded
     * authenticatedAttributes. This method provides that encoding and the parameters must be
     * exactly the same as in {@link #getEncodedPKCS7(byte[])}.
     *
     * <p>
     *     Note: do not pass in the full DER-encoded OCSPResponse object obtained from the responder,
     *     only the DER-encoded IBasicOCSPResponse value contained in the response data.
     *
     * <p>
     * A simple example:
     * <pre>
     * Calendar cal = Calendar.getInstance();
     * PdfPKCS7 pk7 = new PdfPKCS7(key, chain, null, "SHA1", null, false);
     * MessageDigest messageDigest = MessageDigest.getInstance("SHA1");
     * byte[] buf = new byte[8192];
     * int n;
     * InputStream inp = sap.getRangeStream();
     * while ((n = inp.read(buf)) &gt; 0) {
     *    messageDigest.update(buf, 0, n);
     * }
     * byte[] hash = messageDigest.digest();
     * byte[] sh = pk7.getAuthenticatedAttributeBytes(hash, cal);
     * pk7.update(sh, 0, sh.length);
     * byte[] sg = pk7.getEncodedPKCS7(hash, cal);
     * </pre>
     *
     * @param secondDigest the content digest
     * @param sigtype specifies the PKCS7 standard flavor to which created PKCS7SignedData object will adhere:
     *                either basic CMS or CAdES
     * @param ocsp collection of DER-encoded BasicOCSPResponses for the  certificate in the signature certificates
     *             chain, or null if OCSP revocation data is not to be added.
     * @param crlBytes collection of DER-encoded CRL for certificates from the signature certificates chain,
     *                 or null if CRL revocation data is not to be added.
     * @return the byte array representation of the authenticatedAttributes ready to be signed
     * @see <a href="https://datatracker.ietf.org/doc/html/rfc6960#section-4.2.1">RFC 6960 § 4.2.1</a>
     */
    public byte[] getAuthenticatedAttributeBytes(byte[] secondDigest, PdfSigner.CryptoStandard sigtype,
            Collection<byte[]> ocsp, Collection<byte[]> crlBytes) {
        try {
            return getAuthenticatedAttributeSet(secondDigest, ocsp, crlBytes, sigtype)
                    .getEncoded(bouncyCastleFactory.createEncoding().getDer());
        } catch (Exception e) {
            throw new PdfException(e);
        }
    }

    /**
     * This method provides that encoding and the parameters must be
     * exactly the same as in {@link #getEncodedPKCS7(byte[])}.
     *
     * @param secondDigest the content digest
     * @return the byte array representation of the authenticatedAttributes ready to be signed
     */
    private IDERSet getAuthenticatedAttributeSet(byte[] secondDigest, Collection<byte[]> ocsp,
            Collection<byte[]> crlBytes, PdfSigner.CryptoStandard sigtype) {
        try {
            IASN1EncodableVector attribute = bouncyCastleFactory.createEncodableVector();
            IASN1EncodableVector v = bouncyCastleFactory.createEncodableVector();
            v.add(bouncyCastleFactory.createObjectIdentifier(SecurityIDs.ID_CONTENT_TYPE));
            v.add(bouncyCastleFactory.createDERSet(
                    bouncyCastleFactory.createObjectIdentifier(SecurityIDs.ID_PKCS7_DATA)));
            attribute.add(bouncyCastleFactory.createDERSequence(v));
            v = bouncyCastleFactory.createEncodableVector();
            v.add(bouncyCastleFactory.createObjectIdentifier(SecurityIDs.ID_MESSAGE_DIGEST));
            v.add(bouncyCastleFactory.createDERSet(bouncyCastleFactory.createDEROctetString(secondDigest)));
            attribute.add(bouncyCastleFactory.createDERSequence(v));
            boolean haveCrl = false;
            if (crlBytes != null) {
                for (byte[] bCrl : crlBytes) {
                    if (bCrl != null) {
                        haveCrl = true;
                        break;
                    }
                }
            }
            if (ocsp != null && !ocsp.isEmpty() || haveCrl) {
                v = bouncyCastleFactory.createEncodableVector();
                v.add(bouncyCastleFactory.createObjectIdentifier(SecurityIDs.ID_ADBE_REVOCATION));

                IASN1EncodableVector revocationV = bouncyCastleFactory.createEncodableVector();

                if (haveCrl) {
                    IASN1EncodableVector v2 = bouncyCastleFactory.createEncodableVector();
                    for (byte[] bCrl : crlBytes) {
                        if (bCrl == null) {
                            continue;
                        }
                        IASN1InputStream t = bouncyCastleFactory.createInputStream(new ByteArrayInputStream(bCrl));
                        v2.add(t.readObject());
                    }
                    revocationV.add(bouncyCastleFactory.createDERTaggedObject(
                            true, 0, bouncyCastleFactory.createDERSequence(v2)));
                }

                if (ocsp != null && !ocsp.isEmpty()) {
                    IASN1EncodableVector vo1 = bouncyCastleFactory.createEncodableVector();
                    for (byte[] ocspBytes : ocsp) {
                        IDEROctetString doctet = bouncyCastleFactory.createDEROctetString(ocspBytes);
                        IASN1EncodableVector v2 = bouncyCastleFactory.createEncodableVector();
                        IOCSPObjectIdentifiers objectIdentifiers = bouncyCastleFactory.createOCSPObjectIdentifiers();
                        v2.add(objectIdentifiers.getIdPkixOcspBasic());
                        v2.add(doctet);
                        IASN1Enumerated den = bouncyCastleFactory.createEnumerated(0);
                        IASN1EncodableVector v3 = bouncyCastleFactory.createEncodableVector();
                        v3.add(den);
                        v3.add(bouncyCastleFactory.createDERTaggedObject(
                                true, 0, bouncyCastleFactory.createDERSequence(v2)));
                        vo1.add(bouncyCastleFactory.createDERSequence(v3));
                    }
                    revocationV.add(bouncyCastleFactory.createDERTaggedObject(
                            true, 1, bouncyCastleFactory.createDERSequence(vo1)));
                }

                v.add(bouncyCastleFactory.createDERSet(bouncyCastleFactory.createDERSequence(revocationV)));
                attribute.add(bouncyCastleFactory.createDERSequence(v));
            }
            if (sigtype == PdfSigner.CryptoStandard.CADES) {
                v = bouncyCastleFactory.createEncodableVector();
                v.add(bouncyCastleFactory.createObjectIdentifier(SecurityIDs.ID_AA_SIGNING_CERTIFICATE_V2));

                IASN1EncodableVector aaV2 = bouncyCastleFactory.createEncodableVector();
                IAlgorithmIdentifier algoId = bouncyCastleFactory.createAlgorithmIdentifier(
                        bouncyCastleFactory.createObjectIdentifier(digestAlgorithmOid), null);
                aaV2.add(algoId);
                MessageDigest md = SignUtils.getMessageDigest(getHashAlgorithm(), interfaceDigest);
                byte[] dig = md.digest(signCert.getEncoded());
                aaV2.add(bouncyCastleFactory.createDEROctetString(dig));

                v.add(bouncyCastleFactory.createDERSet(bouncyCastleFactory.createDERSequence(
                        bouncyCastleFactory.createDERSequence(bouncyCastleFactory.createDERSequence(aaV2)))));
                attribute.add(bouncyCastleFactory.createDERSequence(v));
            }

            if (signaturePolicyIdentifier != null) {
                IPKCSObjectIdentifiers ipkcsObjectIdentifiers = bouncyCastleFactory.createPKCSObjectIdentifiers();
                IAttribute attr = bouncyCastleFactory.createAttribute(ipkcsObjectIdentifiers.getIdAaEtsSigPolicyId(),
                        bouncyCastleFactory.createDERSet(signaturePolicyIdentifier));
                attribute.add(attr);
            }

            return bouncyCastleFactory.createDERSet(attribute);
        } catch (Exception e) {
            throw new PdfException(e);
        }
    }

    /*
     *	DIGITAL SIGNATURE VERIFICATION
     */

    /**
     * Signature attributes
     */
    private byte[] sigAttr;
    /**
     * Signature attributes (maybe not necessary, but we use it as fallback)
     */
    private byte[] sigAttrDer;

    /**
     * encrypted digest
     */
    private MessageDigest encContDigest; // Stefan Santesson

    /**
     * Indicates if a signature has already been verified
     */
    private boolean verified;

    /**
     * The result of the verification
     */
    private boolean verifyResult;


    // verification

    /**
     * Verifies that signature integrity is intact (or in other words that signed data wasn't modified)
     * by checking that embedded data digest corresponds to the calculated one. Also ensures that signature
     * is genuine and is created by the owner of private key that corresponds to the declared public certificate.
     * <p>
     * Even though signature can be authentic and signed data integrity can be intact,
     * one shall also always check that signed data is not only a part of PDF contents but is actually a complete PDF file.
     * In order to check that given signature covers the current {@link com.itextpdf.kernel.pdf.PdfDocument} please
     * use {@link SignatureUtil#signatureCoversWholeDocument(String)} method.
     *
     * @return <CODE>true</CODE> if the signature checks out, <CODE>false</CODE> otherwise
     * @throws java.security.GeneralSecurityException if this signature object is not initialized properly,
     * the passed-in signature is improperly encoded or of the wrong type, if this signature algorithm is unable to
     * process the input data provided, if the public key is invalid or if security provider or signature algorithm
     * are not recognized, etc.
     */
    public boolean verifySignatureIntegrityAndAuthenticity() throws GeneralSecurityException {
        if (verified)
            return verifyResult;
        if (isTsp) {
            ITimeStampTokenInfo info = timeStampToken.getTimeStampInfo();
            IMessageImprint imprint = info.toASN1Structure().getMessageImprint();
            byte[] md = messageDigest.digest();
            byte[] imphashed = imprint.getHashedMessage();
            verifyResult = Arrays.equals(md, imphashed);
        } else {
            if (sigAttr != null || sigAttrDer != null) {
                final byte[] msgDigestBytes = messageDigest.digest();
                boolean verifyRSAdata = true;
                // Stefan Santesson fixed a bug, keeping the code backward compatible
                boolean encContDigestCompare = false;
                if (rsaData != null) {
                    verifyRSAdata = Arrays.equals(msgDigestBytes, rsaData);
                    encContDigest.update(rsaData);
                    encContDigestCompare = Arrays.equals(encContDigest.digest(), digestAttr);
                }
                boolean absentEncContDigestCompare = Arrays.equals(msgDigestBytes, digestAttr);
                boolean concludingDigestCompare = absentEncContDigestCompare || encContDigestCompare;
                boolean sigVerify = verifySigAttributes(sigAttr) || verifySigAttributes(sigAttrDer);
                verifyResult = concludingDigestCompare && sigVerify && verifyRSAdata;
            } else {
                if (rsaData != null)
                    sig.update(messageDigest.digest());
                verifyResult = sig.verify(digest);
            }
        }
        verified = true;
        return verifyResult;
    }

    private boolean verifySigAttributes(byte[] attr) throws GeneralSecurityException {
        Signature signature = initSignature(signCert.getPublicKey());
        signature.update(attr);
        return signature.verify(digest);
    }

    /**
     * Checks if the timestamp refers to this document.
     *
     * @return true if it checks false otherwise
     * @throws GeneralSecurityException on error
     */
    public boolean verifyTimestampImprint() throws GeneralSecurityException {
        // TODO DEVSIX-6011 ensure this method works correctly
        if (timeStampToken == null) {
            return false;
        }
        ITimeStampTokenInfo info = timeStampToken.getTimeStampInfo();
        IMessageImprint imprint = info.toASN1Structure().getMessageImprint();
        String algOID = info.getHashAlgorithm().getAlgorithm().getId();
        byte[] md = SignUtils.getMessageDigest(DigestAlgorithms.getDigest(algOID)).digest(digest);
        byte[] imphashed = imprint.getHashedMessage();
        return Arrays.equals(md, imphashed);
    }

    // Certificates

    /**
     * All the X.509 certificates in no particular order.
     */
    private Collection<Certificate> certs;

    /**
     * All the X.509 certificates used for the main signature.
     */
    Collection<Certificate> signCerts;

    /**
     * The X.509 certificate that is used to sign the digest.
     */
    private X509Certificate signCert;

    /**
     * Get all the X.509 certificates associated with this PKCS#7 object in no particular order.
     * Other certificates, from OCSP for example, will also be included.
     *
     * @return the X.509 certificates associated with this PKCS#7 object
     */
    public Certificate[] getCertificates() {
        return certs.toArray(new X509Certificate[certs.size()]);
    }

    /**
     * Get the X.509 sign certificate chain associated with this PKCS#7 object.
     * Only the certificates used for the main signature will be returned, with
     * the signing certificate first.
     *
     * @return the X.509 certificates associated with this PKCS#7 object
     */
    public Certificate[] getSignCertificateChain() {
        return signCerts.toArray(new X509Certificate[signCerts.size()]);
    }

    /**
     * Get the X.509 certificate actually used to sign the digest.
     *
     * @return the X.509 certificate actually used to sign the digest
     */
    public X509Certificate getSigningCertificate() {
        return signCert;
    }

    /**
     * Helper method that creates the collection of certificates
     * used for the main signature based on the complete list
     * of certificates and the sign certificate.
     */
    private void signCertificateChain() {
        List<Certificate> cc = new ArrayList<>();
        cc.add(signCert);
        List<Certificate> oc = new ArrayList<>(certs);
        for (int k = 0; k < oc.size(); ++k) {
            if (signCert.equals(oc.get(k))) {
                oc.remove(k);
                --k;
            }
        }
        boolean found = true;
        while (found) {
            X509Certificate v = (X509Certificate) cc.get(cc.size() - 1);
            found = false;
            for (int k = 0; k < oc.size(); ++k) {
                X509Certificate issuer = (X509Certificate) oc.get(k);
                if (SignUtils.verifyCertificateSignature(v, issuer.getPublicKey(), provider)) {
                    found = true;
                    cc.add(oc.get(k));
                    oc.remove(k);
                    break;
                }
            }
        }
        signCerts = cc;
    }

    // Certificate Revocation Lists

    private Collection<CRL> crls;

    /**
     * Get the X.509 certificate revocation lists associated with this PKCS#7 object
     *
     * @return the X.509 certificate revocation lists associated with this PKCS#7 object
     */
    public Collection<CRL> getCRLs() {
        return crls;
    }

    /**
     * Helper method that tries to construct the CRLs.
     */
    void findCRL(IASN1Sequence seq) {
        try {
            crls = new ArrayList<>();
            for (int k = 0; k < seq.size(); ++k) {
                ByteArrayInputStream ar = new ByteArrayInputStream(
                        seq.getObjectAt(k).toASN1Primitive().getEncoded(bouncyCastleFactory.createEncoding().getDer()));
                X509CRL crl = (X509CRL) SignUtils.parseCrlFromStream(ar);
                crls.add(crl);
            }
        } catch (Exception ex) {
            // ignore
        }
    }

    // Online Certificate Status Protocol

    /**
     * BouncyCastle IBasicOCSPResp
     */
    IBasicOCSPResp basicResp;

    /**
     * Gets the OCSP basic response if there is one.
     *
     * @return the OCSP basic response or null
     */
    public IBasicOCSPResp getOcsp() {
        return basicResp;
    }

    /**
     * Checks if OCSP revocation refers to the document signing certificate.
     *
     * @return true if it checks, false otherwise
     */
    public boolean isRevocationValid() {
        if (basicResp == null)
            return false;
        if (signCerts.size() < 2)
            return false;
        try {
            X509Certificate[] cs = (X509Certificate[]) getSignCertificateChain();
            ISingleResp sr = basicResp.getResponses()[0];
            ICertificateID cid = sr.getCertID();
            X509Certificate sigcer = getSigningCertificate();
            X509Certificate isscer = cs[1];
            ICertificateID tis = SignUtils.generateCertificateId(isscer, sigcer.getSerialNumber(), cid.getHashAlgOID());
            return tis.equals(cid);
        } catch (Exception ignored) {
        }
        return false;
    }

    /**
     * Helper method that creates the IBasicOCSPResp object.
     *
     * @param seq
     * @throws IOException
     */
    private void findOcsp(IASN1Sequence seq) throws IOException {
        basicResp = (IBasicOCSPResp) null;
        boolean ret = false;
        while (true) {
            IASN1ObjectIdentifier objectIdentifier = bouncyCastleFactory.createObjectIdentifier(seq.getObjectAt(0));
            IOCSPObjectIdentifiers ocspObjectIdentifiers = bouncyCastleFactory.createOCSPObjectIdentifiers();
            if (objectIdentifier != null
                    && objectIdentifier.getId().equals(ocspObjectIdentifiers.getIdPkixOcspBasic().getId())) {
                break;
            }
            ret = true;
            for (int k = 0; k < seq.size(); ++k) {
                IASN1Sequence nextSeq = bouncyCastleFactory.createSequence(seq.getObjectAt(k));
                if (nextSeq != null) {
                    seq = nextSeq;
                    ret = false;
                    break;
                }
                IASN1TaggedObject tag = bouncyCastleFactory.createTaggedObject(seq.getObjectAt(k));
                if (tag != null) {
                    nextSeq = bouncyCastleFactory.createSequence(tag.getObject());
                    if (nextSeq != null) {
                        seq = nextSeq;
                        ret = false;
                        break;
                    } else {
                        return;
                    }
                }
            }
            if (ret)
                return;
        }
        IASN1OctetString os = bouncyCastleFactory.createOctetString(seq.getObjectAt(1));
        IASN1InputStream inp = bouncyCastleFactory.createInputStream(os.getOctets());
        IBasicOCSPResponse resp = bouncyCastleFactory.createBasicOCSPResponse(inp.readObject());
        basicResp = bouncyCastleFactory.createBasicOCSPResp(resp);
    }

    // Time Stamps

    /**
     * True if there's a PAdES LTV time stamp.
     */
    private boolean isTsp;

    /**
     * True if it's a CAdES signature type.
     */
    private boolean isCades;

    /**
     * BouncyCastle TimeStampToken.
     */
    private ITimeStampToken timeStampToken;

    /**
     * Check if it's a PAdES-LTV time stamp.
     *
     * @return true if it's a PAdES-LTV time stamp, false otherwise
     */
    public boolean isTsp() {
        return isTsp;
    }

    /**
     * Gets the timestamp token if there is one.
     *
     * @return the timestamp token or null
     */
    public ITimeStampToken getTimeStampToken() {
        return timeStampToken;
    }

    /**
     * Gets the timestamp date.
     *
     * In case the signed document doesn't contain timestamp,
     * {@link TimestampConstants#UNDEFINED_TIMESTAMP_DATE} will be returned.
     *
     * @return the timestamp date
     */
    public Calendar getTimeStampDate() {
        if (timeStampToken == null) {
            return (Calendar) TimestampConstants.UNDEFINED_TIMESTAMP_DATE;
        }
        return SignUtils.getTimeStampDate(timeStampToken);
    }

    /**
     * Getter for the filter subtype.
     *
     * @return the filter subtype
     */
    public PdfName getFilterSubtype() {
        return filterSubtype;
    }

    /**
     * Returns the encryption algorithm
     *
     * @return the name of an encryption algorithm
     */
    public String getEncryptionAlgorithm() {
        String encryptAlgo = EncryptionAlgorithms.getAlgorithm(digestEncryptionAlgorithmOid);
        if (encryptAlgo == null)
            encryptAlgo = digestEncryptionAlgorithmOid;
        return encryptAlgo;
    }
}
