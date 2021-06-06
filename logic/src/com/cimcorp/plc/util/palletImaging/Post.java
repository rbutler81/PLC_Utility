package com.cimcorp.plc.util.palletImaging;

import com.cimcorp.misc.math.MeanStandardDeviation;

import java.io.Serializable;
import java.math.BigDecimal;

public class Post implements Serializable {

    private static final long serialVersionUID = 1L;

    Square area;
    MeanStandardDeviation msd;
    int samplesFound;
    BigDecimal sampleSuccessRate;

    public Post(Square area, MeanStandardDeviation msd, int samplesFound, BigDecimal sampleSuccessRate) {
        this.area = area;
        this.msd = msd;
        this.samplesFound = samplesFound;
        this.sampleSuccessRate = sampleSuccessRate;
    }

    public Square getArea() {
        return area;
    }

    public MeanStandardDeviation getMsd() {
        return msd;
    }

    public BigDecimal getSampleSuccessRate() {
        return sampleSuccessRate;
    }
}
