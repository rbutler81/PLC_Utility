package com.cimcorp.plc.util.plcLogger;

import com.cimcorp.communications.threads.Message;
import com.cimcorp.communications.udp.RecvBytesUdp;
import com.cimcorp.configFile.ParamRangeException;
import com.cimcorp.logger.Logger;
import com.cimcorp.misc.helpers.ApplicationSegment;
import com.cimcorp.misc.helpers.ExceptionUtil;

import java.io.IOException;

import static com.cimcorp.plc.util.Main.PATH_SEPARATOR;

public class PlcLogger extends ApplicationSegment {

    static final String LOG_FILE_NAME = "PLC.log";
    static final String TOP_LINE = "Date/Time,Id,Level,Colour,Header,Description";
    static final boolean USE_TIMESTAMP = false;

    static final String BITMAP_HEADER = ",BMP,";
    static final String ETX = ",";
    static final String STX = ",";
    static final String RX_ERROR_MSG = "RX_ERROR, MAL-FORMED MESSAGE: ";

    private Message<String> fromUdpThreadMsg = null;
    private PlcBitmapHandler bh;
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
        this.bitmapPath = path + "ImagesFromPlc" + PATH_SEPARATOR;

        this.port = config.getSingleParamAsInt("Port", 1, 65535);
        this.bitmapsToKeep = config.getSingleParamAsInt("BitmapsToKeep", 1);

        this.udpServer = new RecvBytesUdp(this.port, 485);
        this.fromUdpThreadMsg = udpServer.getMsg();

        this.logger = new Logger(lb);

        this.bh = new PlcBitmapHandler(bitmapPath, this.bitmapsToKeep);

    }

    @Override
    public void run() {

        Thread udpServerThread = new Thread(udpServer, "PlcLoggerUdpListener");
        udpServerThread.start();

        System.out.println("Application Started -- PLC Logger Enabled and Running");

        while (true) {

            try {

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

                            t = RX_ERROR_MSG + t;
                            logger.logAndPrint(t);
                        }
                    }

                }
            } catch (Throwable t) {
                logger.logAndPrint(ExceptionUtil.stackTraceToString(t));
            }
        }
    }
}
