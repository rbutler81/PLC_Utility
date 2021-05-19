package com.cimcorp.plc.util;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;

public class MeanStandardDeviation {

    private BigDecimal mean;
    private BigDecimal stdDeviation;
    private List<Integer> values;

    public MeanStandardDeviation(List<Integer> values) {

        this.values = values;
        if (values.size() > 0) {
            BigDecimal numerator = BigDecimal.ZERO;
            BigDecimal denominator = new BigDecimal(values.size());

            // calculate mean
            for (Integer i : values) {
                numerator = numerator.add(new BigDecimal(i));
            }
            mean = numerator.divide(denominator, 2, RoundingMode.HALF_UP);

            // calculate variance
            numerator = BigDecimal.ZERO;
            for (Integer i : values) {
                numerator = new BigDecimal(i).setScale(2).subtract(mean).pow(2).add(numerator);
            }
            BigDecimal variance = numerator.divide(denominator, 2, RoundingMode.HALF_UP);

            // calculate standard deviation
            MathContext mc = new MathContext(10,RoundingMode.HALF_UP);
            stdDeviation = new BigDecimal(Math.sqrt(variance.setScale(10,RoundingMode.HALF_UP).doubleValue()))
                                .setScale(2,RoundingMode.HALF_UP);

        } else {
            mean = BigDecimal.ZERO;
            stdDeviation = BigDecimal.ZERO;
        }
    }

    public MeanStandardDeviation() {
        this.mean = BigDecimal.ZERO;
        this.stdDeviation = BigDecimal.ZERO;
    }

    public BigDecimal getMean() {
        return mean;
    }

    public BigDecimal getStdDeviation() {
        return stdDeviation;
    }

    public MeanStandardDeviation setMean(BigDecimal mean) {
        this.mean = mean;
        return this;
    }

    public MeanStandardDeviation setStdDeviation(BigDecimal stdDeviation) {
        this.stdDeviation = stdDeviation;
        return this;
    }
}
