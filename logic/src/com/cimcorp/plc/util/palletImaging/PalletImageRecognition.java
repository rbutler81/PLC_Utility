package com.cimcorp.plc.util.palletImaging;

import com.cimcorp.communications.messageHandling.KvpMessageParser;
import com.cimcorp.communications.messageHandling.MessageEventData;
import com.cimcorp.communications.messageHandling.MessageHandler;
import com.cimcorp.communications.tcp.TcpSendAndReceive;
import com.cimcorp.communications.udp.UdpCommunicationParameters;
import com.cimcorp.configFile.Config;
import com.cimcorp.configFile.ParamRangeException;
import com.cimcorp.logger.Logger;
import com.cimcorp.misc.helpers.ApplicationSegment;
import com.cimcorp.misc.helpers.Clone;
import com.cimcorp.misc.helpers.ExceptionUtil;
import com.cimcorp.misc.helpers.TaskTimer;
import com.sun.org.apache.xerces.internal.impl.dv.util.HexBin;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.cimcorp.plc.util.Main.PATH_SEPARATOR;

public class PalletImageRecognition extends ApplicationSegment {

    static final String LOG_FILE_NAME = "PalletImage.log";
    static final String TOP_LINE = "Pallet Image Recognition Log";
    static final String IMAGE_PATH = "Images" + PATH_SEPARATOR;
    static final boolean USE_TIMESTAMP = true;
    static final String IFM_O3D301_CONNECTION_STRING = "1001L000000008\r\n1001T?\r\n";

    private String path;
    private String iniFileName;
    private ImageParameters ip;
    private int imagesToKeep;
    private int threadsToUse;

    public PalletImageRecognition(String path, String iniFileName, String name) throws IOException, ParamRangeException {

        super(path, iniFileName, LOG_FILE_NAME, TOP_LINE, USE_TIMESTAMP);

        this.name = name;
        this.path = path;
        this.iniFileName = iniFileName;
        this.imagesToKeep = config.getSingleParamAsInt("ImagesToKeep",0);

        this.logger = new Logger(lb, "ImageProcessing");
        this.ip = new ImageParameters(config);

        if (this.ip.getThreadsToUse() == 99) {
            threadsToUse = Runtime.getRuntime().availableProcessors();
        } else {
            threadsToUse = this.ip.getThreadsToUse();
        }

    }

    @Override
    public void run() {

        logger.logAndPrint("Application Started -- Image Processing Enabled and Running");
        logger.logAndPrint("Using " + threadsToUse + " Available CPU Cores");

        // get communication parameters
        int localPort = ip.getListenerPort();
        int remotePort = ip.getRemotePort();
        String remoteIp = ip.getRemoteIp();
        int resendDelay = ip.getResendDelay();
        int resendAttempts = ip.getResendAttempts();

        UdpCommunicationParameters udpToPlcParams = new UdpCommunicationParameters(localPort,
                remotePort,
                remoteIp,
                resendDelay,
                resendAttempts);

        // setup message handler to / from the plc
        MessageHandler messageHandler = null;
        try {
            messageHandler = new MessageHandler(udpToPlcParams,
                    new KvpMessageParser(),
                    lb);
        } catch (Throwable t) {
            logger.logAndPrint(ExceptionUtil.stackTraceToString(t));
        }

        // setup camera communications
        String cameraIp = ip.getCameraIp();
        int cameraPort = ip.getCameraPort();
        int cameraTimeout = ip.getCameraTimeout();
        int cameraRetries = ip.getCameraRetries();
        boolean imageReceived = false;

        MessageEventData newMsg = null;

        while (true) {

            Pallet pallet = null;
            boolean palletDataOk = true;

            try {

                // wait here until a new message arrives from the plc
                messageHandler.getReceiveBufferFromRemote().waitUntilNotifiedOrListNotEmpty();
                newMsg = messageHandler.getReceiveBufferFromRemote().getNextMsg();

                logger.logAndPrint("Starting Image Processing, Setting up Pallet...");

                this.config = Config.readIniFile(path + iniFileName);
                ip = new ImageParameters(config);

                pallet = new Pallet(newMsg.getData());
                pallet.setupDetectionParameters(ip);

            } catch (Throwable t) {

                // received message is malformed
                logger.logAndPrint(ExceptionUtil.stackTraceToString(t));
                messageHandler.sendNak(newMsg);
                palletDataOk = false;

            }

            try {

                if (palletDataOk) {

                    // send an ack
                    messageHandler.sendAck(newMsg);

                    // log pallet details
                    logger.logAndPrint(PalletLogging.logSearchDetails(pallet));
                    PalletLogging.logExpectedStacks(pallet, logger);
                    
                    // connect to the camera and trigger an image
                    TcpSendAndReceive tcpConnectToCamera = new TcpSendAndReceive(cameraIp,
                            cameraPort,
                            cameraTimeout,
                            cameraRetries,
                            lb,
                            "Tcp_IFM_Camera");

                    logger.logAndPrint("Requesting Image from IFM Camera...");
                    List<Integer> bytesReceivedFromCamera = tcpConnectToCamera.send(IFM_O3D301_CONNECTION_STRING,
                            ip.getCameraPacketSizeBytes());
                    byte[] imageBytes = new byte[ip.getCameraPacketSizeBytes()];

                    // if the correct amount of bytes were received from the camera
                    if (bytesReceivedFromCamera.size() == ip.getCameraPacketSizeBytes()) {
                        for (int i = 0; i < ip.getCameraPacketSizeBytes(); i++) {
                            imageBytes[i] = new Integer(bytesReceivedFromCamera.get(i)).byteValue();
                        }

                        logger.logAndPrint("Image from IFM Camera Received");
                        imageReceived = true;

                    } else {

                        logger.logAndPrint("Incorrect byte count from IFM camera. Expected: "
                                + ip.getCameraPacketSizeBytes()
                                + " - Received: "
                                + bytesReceivedFromCamera.size());

                        for (int i = 0; i < ip.getCameraPacketSizeBytes(); i++) {
                            imageBytes[i] = -1;
                        }

                        pallet.setAlarm(PalletAlarm.CAMERA_TIMEOUT);

                    }

                    // if an image wasn't received, skip over the image algorithm
                    if (imageReceived) {

                        // start a timer to measure the image algorithm
                        TaskTimer palletImaging = new TaskTimer(("Pallet Imaging"));

                        // load camera data from hex data that's been saved to a text file -- for testing
                        //byte[] imageBytes = readCameraPacketFromFile("CameraPacket.txt");
                        // load camera data from a saved bitmap -- for testing
                        //fakeCameraPhoto(pallet, "tires.png");

                        pallet.setOriginalImage(ImageProcessing.convertByteArrayTo2DIntArray(imageBytes,
                                ip.getCameraResolution_x(),
                                ip.getCameraResolution_y(),
                                ip.getImageDataOffsetBytes(),
                                ip.isFlipImageHorizontally()));

                        logger.logAndPrint("1D -> 2D Mapping Done");

                        pallet.setFilteredAndCorrectedImage(Clone.deepClone(pallet.getOriginalImage()));

                        // filter the original image and flatten it in a boolean image
                        ImageProcessing.filterImageAndConvertToBoolArray(pallet);
                        logger.logAndPrint("Image Filtering Done");

                        // detect edges in the filtered image
                        ImageProcessing.edgeDetection(pallet);
                        logger.logAndPrint("Edge Detection Done");

                        // run the hough transform
                        ImageProcessing.parallelHoughTransform(pallet, threadsToUse);
                        logger.logAndPrint("Hough Transform Done");

                        // look for stacks from the hough data
                        ImageProcessing.findStacks(pallet);
                        logger.logAndPrint("Finding Stacks Done");
                        PalletLogging.logSuspectedStacks(pallet, logger);

                        // match the suspected stacks to the expected stacks
                        ImageProcessing.matchStacks(pallet);
                        logger.logAndPrint("Matching Stacks Done");
                        PalletLogging.logMatchedStacks(pallet, logger);
                        PalletLogging.logUnMatchedStacks(pallet, logger);

                        // calculate the actual stack positions with respect to the pallet
                        ImageProcessing.calculateRealStackPositions(pallet);
                        logger.logAndPrint("Calculated Stack Positions");
                        PalletLogging.logFinalStacks(pallet, logger);
                        PalletLogging.algoStats(pallet, threadsToUse, palletImaging, logger);

                    }

                    // send the response message -- check the ACK queue for messages with the same message id
                    int msgId = messageHandler.determineNextMsgId(pallet.getMsgId());
                    pallet.setMsgId(msgId);
                    messageHandler.sendMessage(pallet.toString(), msgId);


                    // saving images
                    if (imageReceived) {
                        logger.logAndPrint("Saving Images...");
                        String imagePath = path + IMAGE_PATH;
                        PalletBitmap palletBitmap = new PalletBitmap(imagesToKeep, imagePath, pallet.getTrackingNumber());
                        palletBitmap.createBitmap(pallet.getOriginalImage(), "Original", false);
                        palletBitmap.createBitmap(pallet.getBoolImage(), "Filtered");
                        palletBitmap.createBitmap(pallet.getEdgeImage(), "Edge");
                        palletBitmap.mergeLayersAndCreateBitmap(pallet.getHoughLayers(), "Hough");
                        palletBitmap.drawHoughCirclesOnOriginal(pallet, "HoughResult");
                        logger.logAndPrint("Images Saved");
                    }
                }

            } catch (Throwable t) {
                logger.logAndPrint(ExceptionUtil.stackTraceToString(t));
            }
        }
    }

    private byte[] readCameraPacketFromFile(String filename) {

        String cameraPacketString = "";

        try {
            BufferedReader br = new BufferedReader(new FileReader(filename));

            cameraPacketString = br.readLine();

            String line = "";
            while (line != null) {
                line = br.readLine();
                if (line != null) {
                    cameraPacketString = cameraPacketString + line;
                }
            }
            } catch (Throwable t) {

            }

        String[] hexArray = cameraPacketString.split(" ");
        List<byte[]> bytes = new ArrayList<>();
        for (String s: hexArray) {
            bytes.add(HexBin.decode(s));
        }

        byte[] r = new byte[bytes.size()];
        for (int i = 0; i < bytes.size(); i++) {
            r[i] = bytes.get(i)[0];
        }

        return r;
    }

    private void fakeCameraPhoto(Pallet pallet, String filename) {
        BufferedImage img = null;
        try {
            img = ImageIO.read(new File(filename));
        } catch (IOException e) {
        }

        for (int y = 0; y < 264; y++) {
            for (int x = 0; x < 352; x++) {
                int value = img.getRGB(x,y);
                if (value == -1) {
                    pallet.getOriginalImage()[y][x] = 1;
                } else {
                    pallet.getOriginalImage()[y][x] = 3460;
                }
            }
        }
    }
}
