package com.cimcorp.plc.util.plcUtility;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import configFileUtil.ParamRangeException;

public class Main {

    // setup static variables
    static final String VER = "1.0";
    // plc logger variables
    static final String PATH = Paths.get(".").toAbsolutePath().normalize().toString() + "\\";
    static final String PLC_LOGGER_INI = "plclogger.ini";
    static final String PLC_LOGGER_NAME = "PLC Logger";

    public static void main(String[] args) {

        System.out.println("PLC Utility v" + VER);

        List<ApplicationSegment> appSegments = new ArrayList<>();
        // check for config files
        for (String s: args) {
            // if the plclogger.ini is found in the command line arguments
            if (s.equals(PLC_LOGGER_INI)) {
                try {
                    appSegments.add(new PlcLogger(PATH, PLC_LOGGER_INI, PLC_LOGGER_NAME));
                } catch (IOException | ParamRangeException e) {
                    e.printStackTrace();
                    System.exit(0);
                }
            }
        }

        List<Thread> threads = new ArrayList<>();
        // find and start application segments
        for (ApplicationSegment as: appSegments) {
            Thread newThread = new Thread(as, as.getName());
            newThread.start();
            threads.add(newThread);
        }

        // threads should now be running, program will end if all threads end
        for (Thread t: threads) {
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }
}
