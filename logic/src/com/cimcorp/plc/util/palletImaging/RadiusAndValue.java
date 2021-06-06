package com.cimcorp.plc.util.palletImaging;

import java.io.Serializable;
import java.util.concurrent.locks.ReentrantLock;

public class RadiusAndValue implements Serializable {

    private static final long serialVersionUID = 1L;

    private int radius;
    private int value;
    private ReentrantLock lock = new ReentrantLock(false);

    public RadiusAndValue() {
        this.radius = 0;
        this.value = 0;
    }

    public RadiusAndValue(int radius, int value) {
        this.radius = radius;
        this.value = value;
    }

    public int getRadius() {
        while (lock.isLocked()) {}
        lock.lock();
        int radiusToReturn = radius;
        lock.unlock();
        return radiusToReturn;
    }

    public int getValue() {
        while (lock.isLocked()) {}
        lock.lock();
        int valueToReturn = value;
        lock.unlock();
        return valueToReturn;
    }

    public RadiusAndValue setRadius(int radius) {
        while (lock.isLocked()) {}
        lock.lock();
        this.radius = radius;
        lock.unlock();
        return this;
    }

    public RadiusAndValue setValue(int value) {
        while (lock.isLocked()) {}
        lock.lock();
        this.value = value;
        lock.unlock();
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
