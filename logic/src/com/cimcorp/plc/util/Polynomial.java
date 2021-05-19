package com.cimcorp.plc.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public class Polynomial {

    List<BigDecimal> coefficients = new ArrayList<>();
    int highestPower = 0;

    public Polynomial(String[] coefficientList, int scale) {
        this.highestPower = coefficientList.length - 1;
        for (int i = 0; i <= highestPower; i++) {
            coefficients.add(new BigDecimal(coefficientList[i]).setScale(scale, RoundingMode.HALF_UP));
        }
    }

    public BigDecimal calculateForX(BigDecimal x) {
        BigDecimal result = BigDecimal.ZERO;
        for (int i = 0; i <= highestPower; i++) {
            int power = highestPower - i;
            result = x.pow(power).multiply(coefficients.get(i)).add(result);
        }
        return result;
    }



}
