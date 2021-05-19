package com.cimcorp.plc.util;

public class PalletException extends Exception {

    public PalletException(String s) {
        super("Pallet creation error: " + s);
    }
}
