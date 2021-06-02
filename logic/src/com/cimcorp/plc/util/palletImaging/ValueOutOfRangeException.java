package com.cimcorp.plc.util.palletImaging;

public class ValueOutOfRangeException extends Exception {

    ValueOutOfRangeException(String s, int value, int min, int max) {
        super(s + " out of range: " + value + " must be between " + min + " and " + max);
    }
}
