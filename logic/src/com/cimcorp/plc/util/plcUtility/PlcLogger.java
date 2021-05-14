package com.cimcorp.plc.util.plcUtility;

import configFileUtil.ParamRangeException;
import logger.Logger;
import threads.Message;
import udp.RecvBytesUdp;

import java.io.IOException;

public class PlcLogger extends ApplicationSegment {

    static final String LOG_FILE_NAME = "PLC.log";
    static final String TOP_LINE = "Date/Time,Id,Level,Colour,Header,Description";
    static final boolean USE_TIMESTAMP = false;

    static final String BITMAP_HEADER = ",BMP,";
    static final String ETX = ",";
    static final String STX = ",";

    private Message<String> fromUdpThreadMsg = null;
    private BitmapHandler bh;
    private RecvBytesUdp udpServer;
    private String path;
    private String iniFileName;
    private String bitmapPath;
    private int port;
    private int bitmapsToKeep;

    public PlcLogger(String path, String iniFileName, String name) throws IOException, ParamRangeException {

        super(path, iniFileName, LOG_FILE_NAME, TOP_LINE, USE_TIMESTAMP);

        this.name = name;
        this.path = path;
        this.iniFileName = iniFileName;
        this.bitmapPath = path + "BMP\\";


        this.port = config.getSingleParamAsInt("Port", 1, 65535);
        this.bitmapsToKeep = config.getSingleParamAsInt("BitmapsToKeep", 1);

        this.udpServer = new RecvBytesUdp(this.port, 485);
        this.fromUdpThreadMsg = udpServer.getMsg();

        this.logger = new Logger(lb);

        this.bh = new BitmapHandler(bitmapPath, this.bitmapsToKeep);

    }

    @Override
    public void run() {

        Thread udpServerThread = new Thread(udpServer, "PlcLoggerUdpListener");
        udpServerThread.start();

        System.out.println(Thread.currentThread().getName() + " Enabled and Running");

        while (true) {
            // wait on message from the udp listener
            synchronized (fromUdpThreadMsg) {
                fromUdpThreadMsg.waitUntilNotifiedOrListNotEmpty();
            }
            // consume all messages in the udp listener queue
            while (!fromUdpThreadMsg.isEmpty()) {

                String t = fromUdpThreadMsg.getNextMsg();

                int s = t.indexOf(STX);
                int x = t.indexOf(ETX);

                if ((s == 0) && (x > 10)) {

                    t = t.substring(2, x);

                    if (t.contains(BITMAP_HEADER)) {
                        bh.parseLine(t);
                    } else {
                        logger.logAndPrint(t);
                    }
                } else {

                    t = t.concat(" RX_ERROR: MAL-FORMED");
                    logger.logAndPrint(t);
                }
            }

        }
    }
}
