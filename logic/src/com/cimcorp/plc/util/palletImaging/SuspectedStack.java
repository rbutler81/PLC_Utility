package com.cimcorp.plc.util.palletImaging;

import com.cimcorp.misc.math.MeanStandardDeviation;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class SuspectedStack extends Stack {

    private int houghValue = 0;
    private int pixelRadius = 0;
    private MeanStandardDeviation msd;
    private int successfulSamples = 0;
    private int samples = 0;
    private BigDecimal sampleSuccessRate;
    private int measuredDistanceFromCamera = 0;

    public SuspectedStack() {
        super();
        houghValue = 0;
        pixelRadius = 0;
        sampleSuccessRate = BigDecimal.ZERO;
        msd = new MeanStandardDeviation();
        successfulSamples = 0;
        samples = 0;
        measuredDistanceFromCamera = 0;
    }

    public int getHoughValue() {
        return houghValue;
    }

    public SuspectedStack setHoughValue(int houghValue) {
        this.houghValue = houghValue;
        return this;
    }

    public MeanStandardDeviation getMsd() {
        return msd;
    }

    public SuspectedStack setMsd(MeanStandardDeviation msd) {
        this.msd = msd;
        return this;
    }

    public BigDecimal getSampleSuccessRate() {
        return sampleSuccessRate;
    }

    public SuspectedStack setSampleSuccessRate(BigDecimal sampleSuccessRate) {
        this.sampleSuccessRate = sampleSuccessRate;
        return this;
    }

    public int getPixelRadius() {
        return pixelRadius;
    }

    public SuspectedStack setPixelRadius(int pixelRadius) {
        this.pixelRadius = pixelRadius;
        return this;
    }

    public int getMeasuredDistanceFromCamera() {
        return measuredDistanceFromCamera;
    }

    public SuspectedStack setMeasuredDistanceFromCamera(int measuredDistanceFromCamera) {
        this.measuredDistanceFromCamera = measuredDistanceFromCamera;
        return this;
    }

    public int getSuccessfulSamples() {
        return successfulSamples;
    }

    public SuspectedStack setSuccessfulSamples(int successfulSamples) {
        this.successfulSamples = successfulSamples;
        return this;
    }

    public int getSamples() {
        return samples;
    }

    public SuspectedStack setSamples(int samples) {
        this.samples = samples;
        return this;
    }

    public void mergeMeasuredData(MeasuredStackData bestStack) {
        this.getMsd().setMean(bestStack.getMean());
        this.getMsd().setStdDeviation(bestStack.getStdDeviation());
        this.setMeasuredHeight(bestStack.getMeasuredStackHeight());
        this.setMeasuredDistanceFromCamera(bestStack.getMeasuredDistanceFromCamera());
        this.setSamples(bestStack.getSamples());
        this.setSuccessfulSamples(bestStack.getSuccessfulSamples());
        this.setSampleSuccessRate(new BigDecimal(this.successfulSamples)
                                    .divide(new BigDecimal(this.samples),4, RoundingMode.HALF_UP)
                                    .multiply(new BigDecimal(100)).setScale(2));
    }

}
