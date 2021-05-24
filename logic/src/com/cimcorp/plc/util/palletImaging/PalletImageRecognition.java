package com.cimcorp.plc.util.palletImaging;

import com.cimcorp.communications.messageHandling.KVPMessageParser;
import com.cimcorp.communications.messageHandling.MessageEventData;
import com.cimcorp.communications.messageHandling.MessageHandler;
import com.cimcorp.communications.threads.Message;
import com.cimcorp.communications.udp.UdpCommunicationParameters;
import com.cimcorp.configFile.Config;
import com.cimcorp.configFile.ParamRangeException;
import com.cimcorp.logger.Logger;
import com.cimcorp.plc.util.ApplicationSegment;
import com.cimcorp.plc.util.Clone;
import com.cimcorp.plc.util.ExceptionUtil;
import com.cimcorp.plc.util.TaskTimer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class PalletImageRecognition extends ApplicationSegment {

    static final String LOG_FILE_NAME = "PalletImage.log";
    static final String TOP_LINE = "Pallet Image Recognition Log";
    static final boolean USE_TIMESTAMP = true;
    static final int THREADS_TO_USE = Runtime.getRuntime().availableProcessors();

    private String path;
    private String iniFileName;
    private ImageParameters ip;
    private int imagesToKeep;

    public PalletImageRecognition(String path, String iniFileName, String name) throws IOException, ParamRangeException {

        super(path, iniFileName, LOG_FILE_NAME, TOP_LINE, USE_TIMESTAMP);

        this.name = name;
        this.path = path;
        this.iniFileName = iniFileName;
        this.imagesToKeep = config.getSingleParamAsInt("ImagesToKeep",0);

        this.logger = new Logger(lb, "ImageProcessing");
        this.ip = new ImageParameters(config);

    }

    @Override
    public void run() {

        logger.logAndPrint("Application Started -- Image Processing Enabled and Running");

        // get communication parameters
        int localPort = ip.getListenerPort();
        int remotePort = ip.getRemotePort();
        String remoteIp = ip.getRemoteIp();
        int resendDelay = ip.getResendDelay();
        int resendAttempts = ip.getResendAttempts();
        UdpCommunicationParameters udpParams = new UdpCommunicationParameters(localPort,
                remotePort,
                remoteIp,
                resendDelay,
                resendAttempts);

        MessageHandler messageHandler = null;
        try {
            messageHandler = new MessageHandler(false,
                    false,
                    false,
                    udpParams,
                    new KVPMessageParser(),
                    lb);
        } catch (Throwable t) {
            logger.logAndPrint(ExceptionUtil.stackTraceToString(t));
        }

        MessageEventData newMsg = null;

        while (true) {

            Pallet pallet = null;
            boolean palletDataOk = true;

            try {

                // wait here until a new message arrives
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

                    byte[] b = new byte[ip.getCameraPacketSizeBytes()];

                    pallet.setOriginalImage(ImageProcessing.convertByteArrayTo2DIntArray(b, ip.getCameraResolution_x(),
                            ip.getCameraResolution_y(), ip.getImageDataOffsetBytes(),
                            ip.isFlipImageHorizontally()));

                    fakeCameraPhoto(pallet);

                    logger.logAndPrint("1D -> 2D Mapping Done");

                    TaskTimer palletImaging = new TaskTimer(("Pallet Imaging"));
                    pallet.setFilteredAndCorrectedImage(Clone.deepClone(pallet.getOriginalImage()));

                    // filter the original image and flatten it in a boolean image
                    ImageProcessing.filterImageAndConvertToBoolArray(pallet);
                    logger.logAndPrint("Image Filtering Done");

                    // detect edges in the filtered image
                    ImageProcessing.edgeDetection(pallet);
                    logger.logAndPrint("Edge Detection Done");

                    // run the hough transform
                    ImageProcessing.parallelHoughTransform(pallet, THREADS_TO_USE);
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
                    PalletLogging.algoStats(pallet, THREADS_TO_USE, palletImaging, logger);

                    // send the response message -- check the ACK queue for messages with the same message id
                    int msgId = determineMsgId(messageHandler, pallet);
                    pallet.setMsgId(msgId);
                    messageHandler.sendMessage(pallet.toString(), msgId);

                    // saving images
                    logger.logAndPrint("Saving Images...");
                    String imagePath = path + "Images\\";
                    PalletBitmap palletBitmap = new PalletBitmap(imagesToKeep, imagePath, pallet.getTrackingNumber());
                    palletBitmap.createBitmap(pallet.getOriginalImage(), "original", false);
                    palletBitmap.createBitmap(pallet.getBoolImage(), "filtered");
                    palletBitmap.createBitmap(pallet.getEdgeImage(), "edge");
                    palletBitmap.mergeLayersAndCreateBitmap(pallet.getHoughLayers(),"hough");
                    palletBitmap.drawHoughCirclesOnOriginal(pallet, "hough_result");
                    logger.logAndPrint("Images Saved");
                }

            } catch (Throwable t) {
                logger.logAndPrint(ExceptionUtil.stackTraceToString(t));
            }
        }
    }

    private int determineMsgId(MessageHandler messageHandler, Pallet pallet) {
        int r = 0;
        int listSize = messageHandler.getMessagesWaitingForAck().size();
        Message<MessageEventData> unlocked = messageHandler.getMessagesWaitingForAck().lock();
        int proposedMsgId = pallet.getMsgId() + 1;
        boolean done = false;
        while (!done) {
            boolean exists = false;
            for (int i = 0; i < listSize; i++) {
                if (proposedMsgId == unlocked.getListWithoutLocking().get(i).getMsgId()){
                    exists = true;
                    proposedMsgId = proposedMsgId + 1;
                }
            }
            if (!exists) {
                done = true;
                r = proposedMsgId;
            }
        }
        messageHandler.getMessagesWaitingForAck().unlock();
        return r;
    }

    private void fakeCameraPhoto(Pallet pallet) {
        BufferedImage img = null;
        try {
            img = ImageIO.read(new File("tires.bmp"));
        } catch (IOException e) {
        }

        for (int y = 0; y < 264; y++) {
            for (int x = 0; x < 352; x++) {
                int value = img.getRGB(x,y);
                if (value == -1) {
                    pallet.getOriginalImage()[y][x] = 1;
                } else {
                    pallet.getOriginalImage()[y][x] = 4000;
                }
            }
        }
    }
}
