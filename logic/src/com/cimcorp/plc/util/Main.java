package com.cimcorp.plc.util;

import com.cimcorp.configFile.ParamRangeException;
import com.cimcorp.misc.helpers.ApplicationSegment;
import com.cimcorp.plc.util.palletImaging.PalletImageRecognition;
import com.cimcorp.plc.util.palletImaging.ValueOutOfRangeException;
import com.cimcorp.plc.util.plcLogger.PlcLogger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Main {

    // setup static variables
    static final String VER = "2_20210830";
    public static final String PATH_SEPARATOR = File.separator;
    static final String PATH = Paths.get(".").toAbsolutePath().normalize().toString() + PATH_SEPARATOR;
    static final int MAX_APP_SEGMENTS = 2;

    // plc logger variables
    static final String PLC_LOGGER_INI = "plclogger.ini";
    static final String PLC_LOGGER_NAME = "PLC Logger";
    // pallet image recognition variables
    static final String PALLET_IMAGE_INI = "palletimage.ini";
    static final String PALLET_IMAGE_NAME = "Pallet Image Recognition";

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
                    System.exit(10);
                }
            // if the 'palletimage.ini' is found in the command line arguments
            } else if (s.equals(PALLET_IMAGE_INI)) {
                try {
                    appSegments.add(new PalletImageRecognition(PATH, PALLET_IMAGE_INI, PALLET_IMAGE_NAME));
                } catch (IOException | ParamRangeException | ValueOutOfRangeException | ClassNotFoundException e) {
                    e.printStackTrace();
                    System.exit(10);
                }
            }
        }

        ExecutorService es = Executors.newFixedThreadPool(MAX_APP_SEGMENTS);
        // start application segment threads
        for (ApplicationSegment as: appSegments) {
            es.execute(as);
        }
        es.shutdown();

        try {
            es.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
        } catch (InterruptedException e) {
            e.printStackTrace();
            System.exit(10);
        }

        System.exit(0);
    }

}
