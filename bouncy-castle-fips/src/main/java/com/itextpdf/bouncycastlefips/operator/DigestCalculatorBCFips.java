package com.itextpdf.bouncycastlefips.operator;

import com.itextpdf.commons.bouncycastle.operator.IDigestCalculator;

import java.util.Objects;
import org.bouncycastle.operator.DigestCalculator;

public class DigestCalculatorBCFips implements IDigestCalculator {
    private final DigestCalculator digestCalculator;

    public DigestCalculatorBCFips(DigestCalculator digestCalculator) {
        this.digestCalculator = digestCalculator;
    }

    public DigestCalculator getDigestCalculator() {
        return digestCalculator;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DigestCalculatorBCFips that = (DigestCalculatorBCFips) o;
        return Objects.equals(digestCalculator, that.digestCalculator);
    }

    @Override
    public int hashCode() {
        return Objects.hash(digestCalculator);
    }

    @Override
    public String toString() {
        return digestCalculator.toString();
    }
}