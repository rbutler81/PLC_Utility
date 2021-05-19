package com.cimcorp.plc.util;

import configFileUtil.Config;
import configFileUtil.ParamRangeException;
import logger.*;

import java.io.IOException;

public class ApplicationSegment implements Runnable {

    protected String name = null;
    protected Config config = null;
    protected LoggerBase lb = null;
    protected Logger logger = null;
    protected LogConfig lc = null;

    // Constructor to use with a logger
    public ApplicationSegment(String path, String iniFileName,  String logFileName, String topLine, boolean useTimeStamp)  throws IOException, ParamRangeException {

        // setup the logger
        try {
            this.config = Config.readIniFile(path + iniFileName);
        } catch (IOException e) {
            throw e;
        }

        lc = new LogConfig(config.getSingleParamAsInt("MaxLogSizeBytes", 5000),
                            config.getSingleParamAsInt("OldLogsToKeep", 1),
                            path,
                            logFileName,
                            topLine,
                            useTimeStamp);

        lb = new LoggerBase(lc);

    }

    public String getName() {
        return name;
    }

    @Override
    public void run() {

    }
}
