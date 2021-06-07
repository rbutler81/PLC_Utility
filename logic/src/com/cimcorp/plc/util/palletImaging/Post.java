package com.cimcorp.plc.util.palletImaging;

import com.cimcorp.misc.helpers.KeyValuePair;
import com.cimcorp.misc.math.MeanStandardDeviation;

import java.io.Serializable;
import java.math.BigDecimal;

public class Post implements Serializable {

    private static final long serialVersionUID = 1L;

    Square area;
    MeanStandardDeviation msd;
    int samplesFound;
    BigDecimal sampleSuccessRate;
    int postHeight;

    public Post(Square area, MeanStandardDeviation msd, int samplesFound, BigDecimal sampleSuccessRate, int postHeight) {
        this.area = area;
        this.msd = msd;
        this.samplesFound = samplesFound;
        this.sampleSuccessRate = sampleSuccessRate;
        this.postHeight = postHeight;
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

    public String toString() {

        String r = KeyValuePair.kVPToString("SampleSuccessRate", sampleSuccessRate,2)
                + KeyValuePair.kVPToString("Height", postHeight)
                + KeyValuePair.kVPToString("StdDeviation", msd.getStdDeviation(),0);
        return r;

    }
}
