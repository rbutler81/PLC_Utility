package com.cimcorp.plc.util;

import java.util.Calendar;

public class TaskTimer {

    Calendar cal;
    String taskName = "";
    long startTime = 0;

    public TaskTimer(String taskName) {
        this.taskName = taskName;
        cal = Calendar.getInstance();
        this.startTime = cal.getTimeInMillis();
    }

    public long checkTime() {
        cal = Calendar.getInstance();
        return cal.getTimeInMillis() - startTime;
    }

}
