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
import java.io.*;
import java.util.ArrayList;
import java.util.List;

import static com.cimcorp.plc.util.Main.PATH_SEPARATOR;

public class PalletImageRecognition extends ApplicationSegment {

    static final String LOG_FILE_NAME = "PalletImage.log";
    static final String TOP_LINE = "Pallet Image Recognition Log";
    static final String IMAGE_PATH = "Images" + PATH_SEPARATOR;
    static final int INVALID_DATA = -1;
    static final boolean USE_TIMESTAMP = true;
    static final int USE_PHYSICAL_MACHINE_CORES_MAX = 99;
    static final String IFM_O3D301_CONNECTION_STRING = "1001L000000008\r\n1001T?\r\n";

    private String path;
    private String iniFileName;
    private ImageParameters imageParameters;
    private int imagesToKeep;
    private int threadsToUse;
    private boolean debugMode;
    private boolean extractAndSaveIniFile;
    private SerializedPalletDetails dataFromFile;
    private SerializedPalletDetails dataToSave;

    public PalletImageRecognition(String path, String iniFileName, String name) throws IOException, ParamRangeException, ValueOutOfRangeException, ClassNotFoundException {

        super(path, iniFileName, LOG_FILE_NAME, TOP_LINE, USE_TIMESTAMP);

        this.name = name;
        this.path = path;
        this.iniFileName = iniFileName;
        this.imagesToKeep = config.getSingleParamAsInt("ImagesToKeep",0);

        this.logger = new Logger(lb, "ImageProcessing");
        this.imageParameters = new ImageParameters(config);

        // check for debug mode
        this.debugMode = imageParameters.isDebugEnabled();
        this.extractAndSaveIniFile = imageParameters.isExtractIniAsFile() && debugMode;

        if (debugMode) {
            logger.logAndPrint("*** Debug Mode Enabled ***");
            setupDebugMode();
        }

        // determine number of threads to use for hough transform
        if (this.imageParameters.getThreadsToUse() == USE_PHYSICAL_MACHINE_CORES_MAX) {
            threadsToUse = Runtime.getRuntime().availableProcessors();
        } else {
            threadsToUse = this.imageParameters.getThreadsToUse();
        }

    }

    private void setupDebugMode() throws IOException, ClassNotFoundException {
        File data = new File(path + imageParameters.getDebugFilename());

        if (!data.exists()) {
            throw new FileNotFoundException(data.toString());
        } else {

            logger.logAndPrint("Reading " + imageParameters.getDebugFilename() + "...");
            dataFromFile = PalletDataFiles.readSerializedData(data.toString());

            // extract the ini from the data file and save it to disk -- ends program when complete
            if (extractAndSaveIniFile) {

                extractAndSaveIniFile();

            // decide whether to run the algorithm using the ini in the data file, or the ini saved in the program folder
            } else {

                if (imageParameters.useDebugFileParams()) {
                    logger.logAndPrint("Using Parameters from " + imageParameters.getDebugFilename());
                    imageParameters = dataFromFile.getImageParameters();
                } else {
                    logger.logAndPrint("Using Parameters from INI file");
                }
            }
        }
    }

    private void extractAndSaveIniFile() throws IOException {

        logger.logAndPrint("Saving INI File Found in " + imageParameters.getDebugFilename());
        PalletDataFiles iniFileWriter = new PalletDataFiles(imagesToKeep, path, dataFromFile.getTrackingNumber());
        iniFileWriter.saveIniFile(dataFromFile);

    }

    @Override
    public void run() {

        // if INI file was extracted, exit the program
        if (!extractAndSaveIniFile) {

            logger.logAndPrint("Application Started -- Image Processing Enabled and Running");
            logger.logAndPrint("Using " + threadsToUse + " Available CPU Cores");

            MessageHandler messageHandler = null;
            UdpCommunicationParameters udpToPlcParams = null;

            String cameraIp = null;
            int cameraPort = 0;
            int cameraTimeout = 0;
            int cameraRetries = 0;
            boolean imageReceived = false;

            MessageEventData newMsg = null;

            if (!debugMode) {

                // get communication parameters
                int localPort = imageParameters.getListenerPort();
                int remotePort = imageParameters.getRemotePort();
                String remoteIp = imageParameters.getRemoteIp();
                int resendDelay = imageParameters.getResendDelay();
                int resendAttempts = imageParameters.getResendAttempts();

                udpToPlcParams = new UdpCommunicationParameters(localPort,
                        remotePort,
                        remoteIp,
                        resendDelay,
                        resendAttempts);

                // setup message handler to / from the plc
                try {
                    messageHandler = new MessageHandler(udpToPlcParams,
                            new KvpMessageParser(),
                            lb);
                } catch (Throwable t) {
                    logger.logAndPrint(ExceptionUtil.stackTraceToString(t));
                }

                // setup camera communications
                cameraIp = imageParameters.getCameraIp();
                cameraPort = imageParameters.getCameraPort();
                cameraTimeout = imageParameters.getCameraTimeout();
                cameraRetries = imageParameters.getCameraRetries();

            }

            boolean runRoutineForever = true;
            while (runRoutineForever) {

                if (debugMode) {
                    runRoutineForever = false;
                }

                Pallet pallet = null;
                dataToSave = null;
                boolean palletDataOk = true;

                try {

                    String newMsgData = null;

                    if (!debugMode) {

                        // wait here until a new message arrives from the plc
                        messageHandler.getReceiveBufferFromRemote().waitUntilNotifiedOrListNotEmpty();
                        newMsg = messageHandler.getReceiveBufferFromRemote().getNextMsg();

                        this.config = Config.readIniFile(path + iniFileName);
                        imageParameters = new ImageParameters(config);

                        newMsgData = newMsg.getData();

                    } else {
                        // load message string from the data file
                        newMsgData = dataFromFile.getPalletMessage();
                    }

                    logger.logAndPrint("Starting Image Processing, Setting up Pallet...");

                    pallet = new Pallet(newMsgData);
                    pallet.setupDetectionParameters(imageParameters);
                    dataToSave = new SerializedPalletDetails(newMsgData);
                    dataToSave.setImageParameters(imageParameters);

                } catch (Throwable t) {

                    // received message is malformed
                    logger.logAndPrint(ExceptionUtil.stackTraceToString(t));

                    if (!debugMode) {
                        messageHandler.sendNak(newMsg);
                    }

                    palletDataOk = false;

                }

                try {

                    if (palletDataOk) {

                        // send an ack
                        if (!debugMode) {
                            messageHandler.sendAck(newMsg);
                        }

                        // log pallet details
                        logger.logAndPrint(PalletLogging.logSearchDetails(pallet));
                        PalletLogging.logExpectedStacks(pallet, logger);

                        byte[] imageBytes = new byte[imageParameters.getCameraPacketSizeBytes()];
                        List<Integer> bytesReceivedFromCamera = new ArrayList<>();
                        if (!debugMode) {

                            // connect to the camera and trigger an image
                            TcpSendAndReceive tcpConnectToCamera = new TcpSendAndReceive(cameraIp,
                                    cameraPort,
                                    cameraTimeout,
                                    cameraRetries,
                                    lb,
                                    "Tcp_IFM_Camera");

                            logger.logAndPrint("Requesting Image from IFM Camera...");
                            bytesReceivedFromCamera = tcpConnectToCamera.send(IFM_O3D301_CONNECTION_STRING,
                                    imageParameters.getCameraPacketSizeBytes());

                            pallet.setCameraByteArray(bytesReceivedFromCamera);
                            dataToSave.setCameraByteArray(bytesReceivedFromCamera);

                        } else {
                            // set camera bytes from data file
                            pallet.setCameraByteArray(dataFromFile.getCameraByteArray());
                            bytesReceivedFromCamera = dataFromFile.getCameraByteArray();
                        }

                        int byteArraySizeFromCamera = pallet.getCameraByteArray().size();

                        // if the correct amount of bytes were received from the camera
                        if (byteArraySizeFromCamera == imageParameters.getCameraPacketSizeBytes()) {
                            for (int i = 0; i < imageParameters.getCameraPacketSizeBytes(); i++) {
                                imageBytes[i] = new Integer(bytesReceivedFromCamera.get(i)).byteValue();
                            }

                            if (!debugMode) {
                                logger.logAndPrint("Image from IFM Camera Received");
                            } else {
                                logger.logAndPrint("Image Read from Data File");
                            }
                            imageReceived = true;

                        } else {

                            logger.logAndPrint("Incorrect byte count from IFM camera. Expected: "
                                    + imageParameters.getCameraPacketSizeBytes()
                                    + " - Received: "
                                    + byteArraySizeFromCamera);

                            // fill image with invalid data
                            for (int i = 0; i < imageParameters.getCameraPacketSizeBytes(); i++) {
                                imageBytes[i] = INVALID_DATA;
                            }

                            pallet.setAlarm(PalletAlarm.CAMERA_TIMEOUT);

                        }

                        // if an image wasn't received, skip over the image algorithm
                        if (imageReceived) {

                            // start a timer to measure the image algorithm
                            TaskTimer palletImaging = new TaskTimer(("Pallet Imaging"));

                            pallet.setOriginalImage(ImageProcessing.convertByteArrayTo2DIntArray(imageBytes,
                                    imageParameters.getCameraResolution_x(),
                                    imageParameters.getCameraResolution_y(),
                                    imageParameters.getImageDataOffsetBytes(),
                                    imageParameters.isFlipImageHorizontally(),
                                    imageParameters.getMinAcceptableDistanceFromCamera(),
                                    imageParameters.getFloorDistanceFromCamera(),
                                    imageParameters.getMaxAcceptableDistanceBelowFloor()));

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

                        if (!debugMode) {
                            // send the response message -- check the ACK queue for messages with the same message id
                            int msgId = messageHandler.determineNextMsgId(pallet.getMsgId());
                            pallet.setMsgId(msgId);
                            messageHandler.sendMessage(pallet.toString(), msgId);
                        }


                        // saving images
                        if (imageReceived) {
                            logger.logAndPrint("Saving Images...");
                            String imagePath = path + IMAGE_PATH;
                            PalletDataFiles palletDataFiles = new PalletDataFiles(imagesToKeep, imagePath, pallet.getTrackingNumber());
                            palletDataFiles.createBitmap(pallet.getOriginalImage(), "Original", false);
                            palletDataFiles.createBitmap(pallet.getBoolImage(), "Filtered");
                            palletDataFiles.createBitmap(pallet.getEdgeImage(), "Edge");
                            palletDataFiles.mergeLayersAndCreateBitmap(pallet.getHoughLayers(), "Hough");
                            palletDataFiles.drawHoughCirclesOnOriginal(pallet, "HoughResult");
                            logger.logAndPrint("Images Saved");

                            if (!debugMode) {
                                dataToSave.setTrackingNumber(pallet.getTrackingNumber());
                                palletDataFiles.saveSerializedData(dataToSave);
                            }


                        }
                    }

                } catch (Throwable t) {
                    logger.logAndPrint(ExceptionUtil.stackTraceToString(t));
                }
            }
        }
        logger.logAndPrint("Shutting Down Application...");
        logger.stop();
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
