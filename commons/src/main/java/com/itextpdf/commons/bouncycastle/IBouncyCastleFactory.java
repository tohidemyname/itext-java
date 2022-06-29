package com.itextpdf.commons.bouncycastle;

import com.itextpdf.commons.bouncycastle.asn1.IASN1EncodableVector;
import com.itextpdf.commons.bouncycastle.asn1.IASN1Encodable;
import com.itextpdf.commons.bouncycastle.asn1.IASN1Encoding;
import com.itextpdf.commons.bouncycastle.asn1.IASN1Enumerated;
import com.itextpdf.commons.bouncycastle.asn1.IASN1InputStream;
import com.itextpdf.commons.bouncycastle.asn1.IASN1Integer;
import com.itextpdf.commons.bouncycastle.asn1.IASN1ObjectIdentifier;
import com.itextpdf.commons.bouncycastle.asn1.IASN1OctetString;
import com.itextpdf.commons.bouncycastle.asn1.IASN1OutputStream;
import com.itextpdf.commons.bouncycastle.asn1.IASN1Primitive;
import com.itextpdf.commons.bouncycastle.asn1.IASN1Sequence;
import com.itextpdf.commons.bouncycastle.asn1.IASN1Set;
import com.itextpdf.commons.bouncycastle.asn1.IASN1TaggedObject;
import com.itextpdf.commons.bouncycastle.asn1.IDERNull;
import com.itextpdf.commons.bouncycastle.asn1.IDEROctetString;
import com.itextpdf.commons.bouncycastle.asn1.IDERSequence;
import com.itextpdf.commons.bouncycastle.asn1.IDERSet;
import com.itextpdf.commons.bouncycastle.asn1.IDERTaggedObject;
import com.itextpdf.commons.bouncycastle.asn1.cms.IAttribute;
import com.itextpdf.commons.bouncycastle.asn1.cms.IAttributeTable;
import com.itextpdf.commons.bouncycastle.asn1.cms.IContentInfo;
import com.itextpdf.commons.bouncycastle.asn1.esf.ISigPolicyQualifierInfo;
import com.itextpdf.commons.bouncycastle.asn1.esf.ISigPolicyQualifiers;
import com.itextpdf.commons.bouncycastle.asn1.esf.ISignaturePolicyIdentifier;
import com.itextpdf.commons.bouncycastle.asn1.ess.ISigningCertificate;
import com.itextpdf.commons.bouncycastle.asn1.ess.ISigningCertificateV2;
import com.itextpdf.commons.bouncycastle.asn1.ocsp.IBasicOCSPResponse;
import com.itextpdf.commons.bouncycastle.asn1.ocsp.IOCSPObjectIdentifiers;
import com.itextpdf.commons.bouncycastle.asn1.pkcs.IPKCSObjectIdentifiers;
import com.itextpdf.commons.bouncycastle.asn1.x509.IAlgorithmIdentifier;
import com.itextpdf.commons.bouncycastle.asn1.x509.IExtension;
import com.itextpdf.commons.bouncycastle.asn1.x509.IExtensions;
import com.itextpdf.commons.bouncycastle.cert.IX509CertificateHolder;
import com.itextpdf.commons.bouncycastle.cert.jcajce.IJcaX509CertificateConverter;
import com.itextpdf.commons.bouncycastle.cert.jcajce.IJcaX509CertificateHolder;
import com.itextpdf.commons.bouncycastle.cert.ocsp.AbstractOCSPException;
import com.itextpdf.commons.bouncycastle.cert.ocsp.IBasicOCSPResp;
import com.itextpdf.commons.bouncycastle.cert.ocsp.ICertificateID;
import com.itextpdf.commons.bouncycastle.cert.ocsp.IOCSPReqBuilder;
import com.itextpdf.commons.bouncycastle.cms.jcajce.IJcaSimpleSignerInfoVerifierBuilder;
import com.itextpdf.commons.bouncycastle.cms.jcajce.IJceKeyTransEnvelopedRecipient;
import com.itextpdf.commons.bouncycastle.operator.IDigestCalculator;
import com.itextpdf.commons.bouncycastle.operator.jcajce.IJcaContentVerifierProviderBuilder;
import com.itextpdf.commons.bouncycastle.operator.jcajce.IJcaDigestCalculatorProviderBuilder;
import com.itextpdf.commons.bouncycastle.tsp.AbstractTSPException;
import com.itextpdf.commons.bouncycastle.tsp.ITimeStampToken;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.Provider;
import java.security.PrivateKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;

public interface IBouncyCastleFactory {
    IASN1ObjectIdentifier createObjectIdentifier(IASN1Encodable encodable);

    IASN1ObjectIdentifier createObjectIdentifier(String str);

    IASN1InputStream createInputStream(InputStream stream);

    IASN1InputStream createInputStream(byte[] bytes);

    IASN1OctetString createOctetString(IASN1Primitive primitive);

    IASN1OctetString createOctetString(IASN1Encodable encodableWrapper);

    IASN1Sequence createSequence(IASN1Primitive primitive);

    IASN1Sequence createSequence(Object object);

    IASN1Sequence createSequence(IASN1Encodable encodableWrapper);

    IDERSequence createDERSequence(IASN1EncodableVector encodableVector);

    IDERSequence createDERSequence(IASN1Primitive primitive);

    IASN1Sequence createSequenceInstance(Object object);

    IASN1TaggedObject createTaggedObject(IASN1Encodable encodableWrapper);

    IASN1Integer createInteger(IASN1Encodable encodableWrapper);

    IASN1Integer createInteger(int i);

    IASN1Integer createInteger(BigInteger i);

    IASN1Set createSet(IASN1Encodable encodableWrapper);

    IASN1Set createSetInstance(IASN1TaggedObject taggedObject, boolean b);

    IASN1OutputStream createOutputStream(OutputStream stream);

    IDEROctetString createDEROctetString(byte[] bytes);

    IASN1EncodableVector createEncodableVector();

    IDERNull createDERNull();

    IDERTaggedObject createDERTaggedObject(int i, IASN1Primitive primitive);

    IDERTaggedObject createDERTaggedObject(boolean b, int i, IASN1Primitive primitive);

    IDERSet createDERSet(IASN1EncodableVector encodableVector);

    IDERSet createDERSet(IASN1Primitive primitive);

    IDERSet createDERSet(ISignaturePolicyIdentifier identifier);

    IASN1Enumerated createEnumerated(int i);

    IASN1Encoding createEncoding();

    IAttributeTable createAttributeTable(IASN1Set unat);

    IPKCSObjectIdentifiers createPKCSObjectIdentifiers();

    IAttribute createAttribute(IASN1ObjectIdentifier attrType, IASN1Set attrValues);

    IContentInfo createContentInfo(IASN1Sequence sequence);

    ITimeStampToken createTimeStampToken(IContentInfo contentInfo) throws AbstractTSPException, IOException;

    ISigningCertificate createSigningCertificate(IASN1Sequence sequence);

    ISigningCertificateV2 createSigningCertificateV2(IASN1Sequence sequence);

    IBasicOCSPResponse createBasicOCSPResponse(IASN1Primitive primitive);

    IBasicOCSPResp createBasicOCSPResp(IBasicOCSPResponse response);

    IOCSPObjectIdentifiers createOCSPObjectIdentifiers();

    IAlgorithmIdentifier createAlgorithmIdentifier(IASN1ObjectIdentifier algorithm, IASN1Encodable encodable);

    Provider createProvider();

    IJceKeyTransEnvelopedRecipient createJceKeyTransEnvelopedRecipient(PrivateKey privateKey);

    IJcaContentVerifierProviderBuilder createJcaContentVerifierProviderBuilder();

    IJcaSimpleSignerInfoVerifierBuilder createJcaSimpleSignerInfoVerifierBuilder();

    IJcaX509CertificateConverter createJcaX509CertificateConverter();

    IJcaDigestCalculatorProviderBuilder createJcaDigestCalculatorProviderBuilder();

    ICertificateID createCertificateID(IDigestCalculator digestCalculator, IX509CertificateHolder certificateHolder,
                                       BigInteger bigInteger) throws AbstractOCSPException;

    IX509CertificateHolder createX509CertificateHolder(byte[] bytes) throws IOException;

    IJcaX509CertificateHolder createJcaX509CertificateHolder(X509Certificate certificate) 
            throws CertificateEncodingException;

    IExtension createExtension(IASN1ObjectIdentifier objectIdentifier, boolean critical, IASN1OctetString octetString);

    IExtensions createExtensions(IExtension extension);

    IOCSPReqBuilder createOCSPReqBuilder();

    ISigPolicyQualifiers createSigPolicyQualifiers(ISigPolicyQualifierInfo... qualifierInfosBC);
}
