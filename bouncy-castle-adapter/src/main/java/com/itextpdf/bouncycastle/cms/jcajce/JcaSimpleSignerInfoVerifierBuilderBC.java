/*
    This file is part of the iText (R) project.
    Copyright (c) 1998-2022 iText Group NV
    Authors: iText Software.

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
package com.itextpdf.bouncycastle.cms.jcajce;

import com.itextpdf.bouncycastle.cms.SignerInformationVerifierBC;
import com.itextpdf.bouncycastle.operator.OperatorCreationExceptionBC;
import com.itextpdf.commons.bouncycastle.cms.ISignerInformationVerifier;
import com.itextpdf.commons.bouncycastle.cms.jcajce.IJcaSimpleSignerInfoVerifierBuilder;

import java.security.cert.X509Certificate;
import java.util.Objects;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.operator.OperatorCreationException;

/**
 * Wrapper class for {@link JcaSimpleSignerInfoVerifierBuilder}.
 */
public class JcaSimpleSignerInfoVerifierBuilderBC implements IJcaSimpleSignerInfoVerifierBuilder {
    private final JcaSimpleSignerInfoVerifierBuilder verifierBuilder;

    /**
     * Creates new wrapper instance for {@link JcaSimpleSignerInfoVerifierBuilder}.
     *
     * @param verifierBuilder {@link JcaSimpleSignerInfoVerifierBuilder} to be wrapped
     */
    public JcaSimpleSignerInfoVerifierBuilderBC(JcaSimpleSignerInfoVerifierBuilder verifierBuilder) {
        this.verifierBuilder = verifierBuilder;
    }

    /**
     * Gets actual org.bouncycastle object being wrapped.
     *
     * @return wrapped {@link JcaSimpleSignerInfoVerifierBuilder}.
     */
    public JcaSimpleSignerInfoVerifierBuilder getVerifierBuilder() {
        return verifierBuilder;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IJcaSimpleSignerInfoVerifierBuilder setProvider(String provider) {
        verifierBuilder.setProvider(provider);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ISignerInformationVerifier build(X509Certificate certificate) throws OperatorCreationExceptionBC {
        try {
            return new SignerInformationVerifierBC(verifierBuilder.build(certificate));
        } catch (OperatorCreationException e) {
            throw new OperatorCreationExceptionBC(e);
        }
    }

    /**
     * Indicates whether some other object is "equal to" this one. Compares wrapped objects.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        JcaSimpleSignerInfoVerifierBuilderBC that = (JcaSimpleSignerInfoVerifierBuilderBC) o;
        return Objects.equals(verifierBuilder, that.verifierBuilder);
    }

    /**
     * Returns a hash code value based on the wrapped object.
     */
    @Override
    public int hashCode() {
        return Objects.hash(verifierBuilder);
    }

    /**
     * Delegates {@code toString} method call to the wrapped object.
     */
    @Override
    public String toString() {
        return verifierBuilder.toString();
    }
}
