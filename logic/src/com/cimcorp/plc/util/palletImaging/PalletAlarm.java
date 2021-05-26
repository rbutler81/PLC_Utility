package com.cimcorp.plc.util.palletImaging;

public enum PalletAlarm {
    NO_ALARM,
    POST_DETECTED,
    CAMERA_TIMEOUT;

    @Override
    public String toString() {
        if (this == NO_ALARM) {
            return "0";
        } else if (this == POST_DETECTED) {
            return "1";
        } else if (this == CAMERA_TIMEOUT) {
            return "2";
        } else {
            return "-1";
        }
    }
}
