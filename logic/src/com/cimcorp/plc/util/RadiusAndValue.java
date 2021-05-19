package com.cimcorp.plc.util;

public class RadiusAndValue {

    private int radius;
    private int value;

    public RadiusAndValue() {
        this.radius = 0;
        this.value = 0;
    }

    public RadiusAndValue(int radius, int value) {
        this.radius = radius;
        this.value = value;
    }

    public int getRadius() {
        return radius;
    }

    public int getValue() {
        return value;
    }

    public RadiusAndValue setRadius(int radius) {
        this.radius = radius;
        return this;
    }

    public RadiusAndValue setValue(int value) {
        this.value = value;
        return this;
    }

    public static RadiusAndValue[][] createTwoDimensionalArray(int x, int y) {

        RadiusAndValue[][] r = new RadiusAndValue[y][x];
        for (int i = 0; i < y; i++) {
            for (int j = 0; j < x; j++) {
                r[i][j] = new RadiusAndValue();
            }
        }
        return r;
    }

}
