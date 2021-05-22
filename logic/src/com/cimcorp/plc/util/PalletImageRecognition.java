package com.cimcorp.plc.util;

import configFileUtil.ParamRangeException;
import logger.Logger;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class PalletImageRecognition extends ApplicationSegment {

    static final String LOG_FILE_NAME = "PalletImage.log";
    static final String TOP_LINE = "Pallet Image Recognition Log";
    static final boolean USE_TIMESTAMP = true;
    static final int THREADS_TO_USE = 12;//Runtime.getRuntime().availableProcessors();

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

        int tn = 1;

        while (true) {

            try {
                Thread.sleep(1000);

                logger.logAndPrint("Starting");
                logger.logAndPrint("Setting up Pallet...");

                String s = "{msg:123}{TN:" + tn + "}{OD:880}{ID:440}{SW:220}{n1:4}{n2:4}{n3:4}{n4:4}{n5:0}";
                tn = tn + 1;

                Pallet pallet = new Pallet(s);
                pallet.setupDetectionParameters(ip);
                logger.logAndPrint(PalletLogging.logSearchDetails(pallet));
                PalletLogging.logExpectedStacks(pallet, logger);

                byte[] b = new byte[ip.getCameraPacketSizeBytes()];

                pallet.setOriginalImage(ImageProcessing.convertByteArrayTo2DIntArray(b, ip.getCameraResolution_x(),
                        ip.getCameraResolution_y(), ip.getImageDataOffsetBytes(),
                        ip.isFlipImageHorizontally()));

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

                logger.logAndPrint("1D -> 2D Mapping Done");

                TaskTimer palletImaging = new TaskTimer(("Pallet Imaging"));
                pallet.setFilteredAndCorrectedImage(Clone.deepClone(pallet.getOriginalImage()));

                ImageProcessing.filterImageAndConvertToBoolArray(pallet);
                logger.logAndPrint("Image Filtering Done");

                ImageProcessing.edgeDetection(pallet);
                logger.logAndPrint("Edge Detection Done");

                //ImageProcessing.houghTransform(pallet);
                ImageProcessing.parallelHoughTransform(pallet, THREADS_TO_USE);
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
                PalletLogging.algoStats(pallet,THREADS_TO_USE,palletImaging,logger);

                // saving images
                logger.logAndPrint("Saving Images...");
                PalletBitmap.createBitmap(pallet.getOriginalImage(), "original", false);
                PalletBitmap.createBitmap(pallet.getBoolImage(), "filtered");
                PalletBitmap.createBitmap(pallet.getEdgeImage(), "edge");
                PalletBitmap.mergeLayersAndCreateBitmap(pallet.getHoughLayers(), "hough");
                PalletBitmap.drawHoughCirclesOnOriginal(pallet, "hough_result");
                logger.logAndPrint("Images Saved");
                System.out.println();

            } catch (KeyValuePairException | PalletException e) {
                logger.logAndPrint(ExceptionUtil.stackTraceToString(e));
            } catch (Throwable t) {
                logger.logAndPrint(ExceptionUtil.stackTraceToString(t));
            }
        }
    }
}
