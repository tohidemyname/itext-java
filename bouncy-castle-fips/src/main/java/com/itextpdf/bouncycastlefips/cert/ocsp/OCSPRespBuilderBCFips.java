package com.itextpdf.bouncycastlefips.cert.ocsp;

import com.itextpdf.commons.bouncycastle.cert.ocsp.IOCSPRespBuilder;

import java.util.Objects;
import org.bouncycastle.cert.ocsp.OCSPRespBuilder;

public class OCSPRespBuilderBCFips implements IOCSPRespBuilder {
    private static final OCSPRespBuilderBCFips INSTANCE = new OCSPRespBuilderBCFips(null);

    private static final int SUCCESSFUL = OCSPRespBuilder.SUCCESSFUL;

    private final OCSPRespBuilder ocspRespBuilder;

    public OCSPRespBuilderBCFips(OCSPRespBuilder ocspRespBuilder) {
        this.ocspRespBuilder = ocspRespBuilder;
    }

    public static OCSPRespBuilderBCFips getInstance() {
        return INSTANCE;
    }

    public OCSPRespBuilder getOcspRespBuilder() {
        return ocspRespBuilder;
    }

    @Override
    public int getSuccessful() {
        return SUCCESSFUL;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        OCSPRespBuilderBCFips that = (OCSPRespBuilderBCFips) o;
        return Objects.equals(ocspRespBuilder, that.ocspRespBuilder);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ocspRespBuilder);
    }

    @Override
    public String toString() {
        return ocspRespBuilder.toString();
    }
}