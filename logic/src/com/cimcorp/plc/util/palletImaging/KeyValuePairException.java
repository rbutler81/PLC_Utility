package com.cimcorp.plc.util.palletImaging;

public class KeyValuePairException extends Exception {

    public KeyValuePairException(String s, String c, int index) {
        super("Char '" + c + "' not found at index " + index + ", in string '" + s + "'");
    }

    public KeyValuePairException(String s, int len) {
        super("String '" + s + "' must be at least " + len + " characters" );
    }

    public KeyValuePairException(int qty, String s, String c) {
        super("String '" + s + "' incorrect quantity of '" + c + "', should have " + qty);
    }

    public KeyValuePairException(String s, int index, String c) {
        super("String '" + s + "', '" + c + "' found before index " + index);
    }

    public KeyValuePairException(String s, int index, String c, boolean b) {
        super("String '" + s + "', '" + c + "' found at, or after index " + index);
    }

    public KeyValuePairException(String s, Throwable t) {
        super(s + "," + t.toString());
    }

    public KeyValuePairException(KeyValuePair k) {
        super("Unexpected value: " + k.toString());
    }
}
