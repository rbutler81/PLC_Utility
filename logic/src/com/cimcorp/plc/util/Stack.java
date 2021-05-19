package com.cimcorp.plc.util;

import java.math.BigDecimal;

public class Stack {

    private boolean stackMatched;
    protected int stackId;
    private int tireQty;
    private int expectedHeight;
    protected int measuredHeight;
    protected int xPixel;
    protected int yPixel;
    private BigDecimal xDistanceFromCenter_mm = BigDecimal.ZERO;
    private BigDecimal yDistanceFromCenter_mm = BigDecimal.ZERO;
    private BigDecimal xDistanceFromCenterAdjusted_mm = BigDecimal.ZERO;
    private BigDecimal yDistanceFromCenterAdjusted_mm = BigDecimal.ZERO;
    private int xDistanceFromPalletOrigin_mm = 0;
    private int yDistanceFromPalletOrigin_mm = 0;
    private SuspectedStack fromSuspectedStack;

    public Stack() {
        stackId = 0;
        tireQty = 0;
        expectedHeight = 0;
        measuredHeight = 0;
        xPixel = -1;
        yPixel =-1;
    }

    public Stack(int stackId, int tireQty) {
        this.stackId = stackId;
        this.tireQty = tireQty;
    }

    public int getStackId() {
        return stackId;
    }

    public int getTireQty() {
        return tireQty;
    }

    public int getExpectedHeight() {
        return expectedHeight;
    }

    public int getMeasuredHeight() {
        return measuredHeight;
    }

    public void setExpectedHeight(int sw) {
        this.expectedHeight = tireQty * sw;
    }

    public Stack setStackId(int stackId) {
        this.stackId = stackId;
        return this;
    }

    public Stack setTireQty(int tireQty) {
        this.tireQty = tireQty;
        return this;
    }

    public Stack setMeasuredHeight(int measuredHeight) {
        this.measuredHeight = measuredHeight;
        return this;
    }

    public int getxPixel() {
        return xPixel;
    }

    public Stack setxPixel(int xPixel) {
        this.xPixel = xPixel;
        return this;
    }

    public int getyPixel() {
        return yPixel;
    }

    public Stack setyPixel(int yPixel) {
        this.yPixel = yPixel;
        return this;
    }

    public SuspectedStack getFromSuspectedStack() {
        return fromSuspectedStack;
    }

    public Stack setFromSuspectedStack(SuspectedStack fromSuspectedStack) {
        this.fromSuspectedStack = fromSuspectedStack;
        return this;
    }

    public boolean isStackMatched() {
        return stackMatched;
    }

    public Stack setStackMatched(boolean stackMatched) {
        this.stackMatched = stackMatched;
        return this;
    }

    public BigDecimal getxDistanceFromCenter_mm() {
        return xDistanceFromCenter_mm;
    }

    public Stack setxDistanceFromCenter_mm(BigDecimal xDistanceFromCenter_mm) {
        this.xDistanceFromCenter_mm = xDistanceFromCenter_mm;
        return this;
    }

    public BigDecimal getyDistanceFromCenter_mm() {
        return yDistanceFromCenter_mm;
    }

    public Stack setyDistanceFromCenter_mm(BigDecimal yDistanceFromCenter_mm) {
        this.yDistanceFromCenter_mm = yDistanceFromCenter_mm;
        return this;
    }

    public int getxDistanceFromPalletOrigin_mm() {
        return xDistanceFromPalletOrigin_mm;
    }

    public Stack setxDistanceFromPalletOrigin_mm(int xDistanceFromPalletOrigin_mm) {
        this.xDistanceFromPalletOrigin_mm = xDistanceFromPalletOrigin_mm;
        return this;
    }

    public int getyDistanceFromPalletOrigin_mm() {
        return yDistanceFromPalletOrigin_mm;
    }

    public Stack setyDistanceFromPalletOrigin_mm(int yDistanceFromPalletOrigin_mm) {
        this.yDistanceFromPalletOrigin_mm = yDistanceFromPalletOrigin_mm;
        return this;
    }

    public BigDecimal getxDistanceFromCenterAdjusted_mm() {
        return xDistanceFromCenterAdjusted_mm;
    }

    public Stack setxDistanceFromCenterAdjusted_mm(BigDecimal xDistanceFromCenterAdjusted_mm) {
        this.xDistanceFromCenterAdjusted_mm = xDistanceFromCenterAdjusted_mm;
        return this;
    }

    public BigDecimal getyDistanceFromCenterAdjusted_mm() {
        return yDistanceFromCenterAdjusted_mm;
    }

    public Stack setyDistanceFromCenterAdjusted_mm(BigDecimal yDistanceFromCenterAdjusted_mm) {
        this.yDistanceFromCenterAdjusted_mm = yDistanceFromCenterAdjusted_mm;
        return this;
    }

}
