package com.cimcorp.plc.util.palletImaging;

public class PalletException extends Exception {

    public PalletException(String s) {
        super("Pallet creation error: " + s);
    }

    public PalletException(String s, int i) {
        super("Pallet creation error, tire stack out of range: " + i +
                " : " +
                s);
    }
}
