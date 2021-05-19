package com.cimcorp.plc.util;

import java.math.BigDecimal;

public class MeasuredStackData {

    int successfulSamples;
    int samples;
    BigDecimal mean;
    BigDecimal stdDeviation;
    int measuredStackHeight;
    int measuredDistanceFromCamera;

    public MeasuredStackData() {

        successfulSamples = 0;
        mean = BigDecimal.ZERO;
        stdDeviation = BigDecimal.ZERO;
        measuredStackHeight = 0;
        measuredDistanceFromCamera = 0;
    }

    public int getSuccessfulSamples() {
        return successfulSamples;
    }

    public MeasuredStackData setSuccessfulSamples(int successfulSamples) {
        this.successfulSamples = successfulSamples;
        return this;
    }

    public BigDecimal getMean() {
        return mean;
    }

    public MeasuredStackData setMean(BigDecimal mean) {
        this.mean = mean;
        return this;
    }

    public BigDecimal getStdDeviation() {
        return stdDeviation;
    }

    public MeasuredStackData setStdDeviation(BigDecimal stdDeviation) {
        this.stdDeviation = stdDeviation;
        return this;
    }

    public int getMeasuredStackHeight() {
        return measuredStackHeight;
    }

    public MeasuredStackData setMeasuredStackHeight(int measuredStackHeight) {
        this.measuredStackHeight = measuredStackHeight;
        return this;
    }

    public int getMeasuredDistanceFromCamera() {
        return measuredDistanceFromCamera;
    }

    public MeasuredStackData setMeasuredDistanceFromCamera(int measuredDistanceFromCamera) {
        this.measuredDistanceFromCamera = measuredDistanceFromCamera;
        return this;
    }

    public int getSamples() {
        return samples;
    }

    public MeasuredStackData setSamples(int samples) {
        this.samples = samples;
        return this;
    }

}
