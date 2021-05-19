package com.cimcorp.plc.util;

import configFileUtil.ParamRangeException;
import logger.Logger;

import java.io.IOException;

public class PalletImageRecognition extends ApplicationSegment {

    static final String LOG_FILE_NAME = "PalletImage.log";
    static final String TOP_LINE = "Pallet Image Recognition Log";
    static final boolean USE_TIMESTAMP = true;

    private String path;
    private String iniFileName;
    private ImageParameters ip;

    public PalletImageRecognition(String path, String iniFileName, String name) throws IOException, ParamRangeException {

        super(path, iniFileName, LOG_FILE_NAME, TOP_LINE, USE_TIMESTAMP);

        this.name = name;
        this.path = path;
        this.iniFileName = iniFileName;

        this.logger = new Logger(lb, "ImageProcessing");

        this.ip = new ImageParameters(config);

    }

    @Override
    public void run() {

        try {
            logger.logAndPrint("Starting");

            logger.logAndPrint("Setting up Pallet...");

            String s = "{msg:123}{TN:5555}{OD:880}{ID:450}{SW:220}{n1:5}{n2:5}{n3:4}{n4:5}{n5:5}";
            Pallet pallet = new Pallet(s);
            pallet.setupDetectionParameters(ip);
            logger.logAndPrint(PalletLogging.logSearchDetails(pallet));
            PalletLogging.logExpectedStacks(pallet, logger);

            byte[] b = new byte[ip.getCameraPacketSizeBytes()];
            byte j = 0;
            for (Byte c: b) {
                c = j;
                j = (byte) (j + 1);
            }

            pallet.setOriginalImage(ImageProcessing.convertByteArrayTo2DIntArray(b,ip.getCameraResolution_x(),
                                                        ip.getCameraResolution_y(),ip.getImageDataOffsetBytes(),
                                                        ip.isFlipImageHorizontally()));
            logger.logAndPrint("1D -> 2D Mapping Done");

            pallet.setFilteredAndCorrectedImage(Clone.deepClone(pallet.getOriginalImage()));

            pallet.setValueInFilteredAndCorrectedImage(10,5, 3500);
            pallet.setValueInFilteredAndCorrectedImage(10,6, 3700);
            pallet.setValueInFilteredAndCorrectedImage(12,5, 3100);

            ImageProcessing.filterImageAndConvertToBoolArray(pallet);
            logger.logAndPrint("Image Filtering Done");
            TestBitmap.test(pallet.getFilteredAndCorrectedImage());

            ImageProcessing.edgeDetection(pallet);
            logger.logAndPrint("Edge Detection Done");

            ImageProcessing.houghTransform(pallet);
            logger.logAndPrint("Hough Transform Done");

            ImageProcessing.findStacks(pallet);
            logger.logAndPrint("Finding Stacks Done");
            PalletLogging.logSuspectedStacks(pallet, logger);

            ImageProcessing.matchStacks(pallet);
            logger.logAndPrint("Matching Stacks Done");
            PalletLogging.logMatchedStacks(pallet, logger);
            PalletLogging.logUnMatchedStacks(pallet, logger);

            ImageProcessing.calculateRealStackPositions(pallet);
            logger.logAndPrint("Calculated Stack Positions");
            PalletLogging.logFinalStacks(pallet, logger);

            System.out.println();
        } catch (KeyValuePairException | PalletException e) {
            logger.logAndPrint(e.toString());
        }

    }
}
