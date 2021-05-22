package com.cimcorp.plc.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public class KeyValuePair {

    private String key;
    private String value;

    public KeyValuePair() {
    }

    public KeyValuePair(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public KeyValuePair(String key, long value) {
        this.key = key;
        this.value = Long.toString(value);
    }

    public KeyValuePair(String key, int value) {
        this.key = key;
        this.value = Integer.toString(value);
    }

    public KeyValuePair(String key, BigDecimal value, int scale) {
        this.key = key;
        this.value = value.setScale(scale, RoundingMode.HALF_UP).toString();
    }

    public KeyValuePair(String s) throws KeyValuePairException {

        if  (isLenGreaterThan(s,5) &&
            findCharAt(s,"{",0) &&
            findCharAt(s,"}",(s.length() - 1)) &&
            hasCharsQtyOf(s,":",1) &&
            hasCharsQtyOf(s,"{",1) &&
            hasCharsQtyOf(s,"}",1) &&
            findCharAtIndexGreaterThanEqual(s,":",2) &&
            findCharAtIndexLessThan(s,":",s.length()-2)) {

            String sub = s.substring(1,s.length()-1);
            String[] keyValue = sub.split(":");
            this.key = keyValue[0];
            this.value = keyValue[1];

        }
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    public boolean isKey(String s) {
        return s.equals(key);
    }

    public int getValueAsInteger() throws KeyValuePairException {
        int r = 0;
        try {
            r = Integer.parseInt(value);
        } catch (Throwable t) {
            throw new KeyValuePairException("Error: " + t.getMessage(), t);
        }
        return r;
    }

    public String toString() {
        return "{" + key + ":" + value + "}";
    }

    // static contructors

    public static List<KeyValuePair> keyValuePairsFromStringList(String s) throws KeyValuePairException {
        List<KeyValuePair> l = new ArrayList<>();
        int start = 0;
        int end = 0;
        boolean done = false;

        while (!done) {

            int indexFrom = s.indexOf("{", start);
            int indexTo = s.indexOf("}", end);

            if ((indexFrom >= start) && (indexTo > indexFrom)) {
                String sub = s.substring(indexFrom, indexTo+1);
                l.add(new KeyValuePair(sub));
                start = indexTo + 1;
                end = start;
            } else {
                done = true;
            }
        }
        return l;
    }

    // private static methods

    private static boolean findCharAt(String s, String c, int index) throws KeyValuePairException {
        String t = Character.toString(s.charAt(index));
        if (t.equals(c)) {
            return true;
        } else {
            throw new KeyValuePairException(s, c, index);
        }
    }

    private static boolean findCharAtIndexLessThan(String s, String c, int index) throws KeyValuePairException {
        int atIndex = s.indexOf(c);
        if ((atIndex >= 0) && (atIndex < index)) {
            return true;
        } else {
            throw new KeyValuePairException(s, index, c, true);
        }

    }

    private static boolean findCharAtIndexGreaterThanEqual(String s, String c, int index) throws KeyValuePairException {
        int atIndex = s.indexOf(c);
        if ((atIndex >= 0) && (atIndex >= index)) {
            return true;
        } else {
            throw new KeyValuePairException(s, index, c);
        }

    }

    private static boolean isLenGreaterThan(String s, int len) throws KeyValuePairException {
        if (s.length() > len) {
            return true;
        } else {
            throw new KeyValuePairException(s, len);
        }
    }

    private static boolean hasCharsQtyOf(String s, String c, int qty) throws KeyValuePairException {
        if (hasChars(s,c) == qty) {
            return true;
        } else {
            throw new KeyValuePairException(qty,s,c);
        }
    }

    private static int hasChars(String s, String c) {
        boolean done = false;
        int index = 0;
        int r = 0;
        int foundAt = 0;
        while (!done) {

            foundAt = s.indexOf(c,index);
            if (foundAt >= index) {
                r = r + 1;
                index = foundAt + 1;
                done = (index == s.length());
            } else {
                done = true;
            }
        }
        return r;
    }

    public static String kVPToString(String k, String v) {
        return new KeyValuePair(k, v).toString();
    }

    public static String kVPToString(String k, int v) {
        return new KeyValuePair(k, v).toString();
    }

    public static String kVPToString(String k, long v) {
        return new KeyValuePair(k, v).toString();
    }

    public static String kVPToString(String k, BigDecimal v, int scale) {
        return new KeyValuePair(k, v, scale).toString();
    }
}
