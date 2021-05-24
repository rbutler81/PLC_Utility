package com.cimcorp.plc.util.palletImaging;

public class HoughMessage {

    int radius = 0;
    int[][] houghArray;

    public HoughMessage(int radius, int[][] houghArray) {
        this.radius = radius;
        this.houghArray = houghArray;
    }

    public int getRadius() {
        return radius;
    }

    public HoughMessage setRadius(int radius) {
        this.radius = radius;
        return this;
    }

    public int[][] getHoughArray() {
        return houghArray;
    }

    public HoughMessage setHoughArray(int[][] houghArray) {
        this.houghArray = houghArray;
        return this;
    }
}
