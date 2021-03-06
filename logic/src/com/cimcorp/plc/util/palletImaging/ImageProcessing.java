package com.cimcorp.plc.util.palletImaging;

import com.cimcorp.communications.threads.Message;
import com.cimcorp.misc.helpers.Clone;
import com.cimcorp.misc.math.BigDecimalMath;
import com.cimcorp.misc.math.MeanStandardDeviation;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ImageProcessing {

    private static final int INVALID_DATA = -1;
    private static final int ERROR_DATA = 0;

    public static int[][] convertByteArrayTo2DIntArray(byte[] inputArray, int xRes, int yRes, int startingOffset, boolean flipImageHorizontally, boolean flipImageVertically, int minAcceptableDistance, int floorDistance, int maxAcceptableDistanceBelowFloor) {

        int[][] outputArray = new int[yRes][xRes];
        int readPointer = startingOffset;
        int xAdjusted;
        int yAdjusted;

        for (int y = 0; y < yRes; y++) {
            for (int x = 0; x < xRes; x++) {

                if (flipImageHorizontally) {
                    xAdjusted = xRes - 1 - x;
                } else {
                    xAdjusted = x;
                }

                if (flipImageVertically) {
                    yAdjusted = yRes - 1 - y;
                } else {
                    yAdjusted = y;
                }

                MathContext mc = new MathContext(0, RoundingMode.HALF_UP);
                BigDecimal highByte = new BigDecimal(Byte.toUnsignedInt(inputArray[readPointer + 1]),mc);
                BigDecimal lowByte = new BigDecimal(Byte.toUnsignedInt(inputArray[readPointer]), mc);
                BigDecimal result = new BigDecimal(2,mc).pow(8).multiply(highByte).add(lowByte);

                int resultInt = result.intValue();

                if (((resultInt >= minAcceptableDistance) && (resultInt <= floorDistance + maxAcceptableDistanceBelowFloor))
                        || resultInt == ERROR_DATA) {
                    outputArray[yAdjusted][xAdjusted] = resultInt;
                } else {
                    outputArray[yAdjusted][xAdjusted] = INVALID_DATA;
                }

                readPointer = readPointer + 2;
            }
        }
        return outputArray;
    }

    public static void filterImageAndConvertToBoolArray(Pallet p) {

        // adjust the height thresholds if height error correction is enabled
        if (p.getImageParameters().isErrorCorrectionHeightAdjustmentEnabled()) {
            // (diHeightThresholdMax_mm - ErrorFactor_b) / ErrorFactor_m
            MathContext mc = new MathContext(0,RoundingMode.HALF_UP);
            p.setHeightThresholdMin(new BigDecimal(p.getHeightThresholdMin(),mc)
                                    .subtract(p.getImageParameters().getErrorCorrectionHeightAdjustment_b())
                                    .divide(p.getImageParameters().getErrorCorrectionHeightAdjustment_m(),0,RoundingMode.HALF_UP)
                                    .intValue());
            p.setHeightThresholdMax(new BigDecimal(p.getHeightThresholdMax(),mc)
                                    .subtract(p.getImageParameters().getErrorCorrectionHeightAdjustment_b())
                                    .divide(p.getImageParameters().getErrorCorrectionHeightAdjustment_m(),0,RoundingMode.HALF_UP)
                                    .intValue());
        }

        // if the max threshold distance is lower than the pallet height, adjust it higher
        if (p.getHeightThresholdMax() > (p.getImageParameters().getFloorDistanceFromCamera() - p.getImageParameters().getPalletHeightFromFloor())) {
            p.setHeightThresholdMax(p.getImageParameters().getFloorDistanceFromCamera() - p.getImageParameters().getPalletHeightFromFloor() - 50);
        }

        p.setBoolImage(new boolean[p.getImageParameters().getCameraResolution_y()][p.getImageParameters().getCameraResolution_x()]);

        int topLeft_x = p.getImageParameters().getCropTopLeft_x();
        int topLeft_y = p.getImageParameters().getCropTopLeft_y();
        int bottomRight_x = p.getImageParameters().getCropBottomRight_x();
        int bottomRight_y = p.getImageParameters().getCropBottomRight_y();

        // iterate through the raw data array and create the boolean image array
        for (int y = 0; y < p.getImageParameters().getCameraResolution_y(); y++) {
            for (int x = 0; x < p.getImageParameters().getCameraResolution_x(); x++) {
                // check to make sure point is within the cropped area
                if (valIsBetween(topLeft_x,x,bottomRight_x) && valIsBetween(topLeft_y,y,bottomRight_y)) {

                    // if missing data error correction is enabled find the average value in an area and 'fill in the holes'
                    if (p.getImageParameters().isErrorCorrectionAverageMissingDataEnabled() && (p.getOriginalImage()[y][x] == 0)) {

                        int xStart, xEnd, yStart, yEnd;
                        int blockSize = p.getImageParameters().getErrorCorrectionBlockSize();
                        boolean blockSizeIsEven = ((blockSize % 2) == 0);

                        if (blockSizeIsEven) {

                            xStart = x - (blockSize / 2);
                            xEnd = x + (blockSize / 2) - 1;
                            xStart = limitInt(0, xStart, p.getImageParameters().getCameraResolution_x() - 1);
                            xEnd = limitInt(0, xEnd, p.getImageParameters().getCameraResolution_x() - 1);

                            yStart = y - (blockSize / 2);
                            yEnd = y + (blockSize / 2) - 1;
                            yStart = limitInt(0, yStart, p.getImageParameters().getCameraResolution_y() - 1);
                            yEnd = limitInt(0, yEnd, p.getImageParameters().getCameraResolution_y() - 1);

                        } else {

                            xStart = x - (blockSize / 2);
                            xEnd = x + (blockSize / 2);
                            xStart = limitInt(0, xStart, p.getImageParameters().getCameraResolution_x() - 1);
                            xEnd = limitInt(0, xEnd, p.getImageParameters().getCameraResolution_x() - 1);

                            yStart = y - (blockSize / 2);
                            yEnd = y + (blockSize / 2);
                            yStart = limitInt(0, yStart, p.getImageParameters().getCameraResolution_y() - 1);
                            yEnd = limitInt(0, yEnd, p.getImageParameters().getCameraResolution_y() - 1);

                        }

                        // gather non-zero values from the block area, calculate average
                        List<Integer> valuesToAverage = new ArrayList<>();
                        for (int i = yStart; i <= yEnd; i++) {
                            for (int j = xStart; j <= xEnd; j++) {

                                if ((p.getHeightThresholdMax() > p.getOriginalImage()[i][j]) && (p.getHeightThresholdMin() < p.getOriginalImage()[i][j])) {
                                    valuesToAverage.add(p.getOriginalImage()[i][j]);
                                }
                            }
                        }
                        MeanStandardDeviation msd = new MeanStandardDeviation(valuesToAverage);
                        p.setValueInFilteredAndCorrectedImage(x, y, msd.getMean().setScale(0, RoundingMode.HALF_UP).intValue());
                    }

                    // map values in the raw image to 'true' or 'false' in the boolean image
                    if ((p.getHeightThresholdMax() > p.getFilteredAndCorrectedImage()[y][x]) && (p.getHeightThresholdMin() < p.getFilteredAndCorrectedImage()[y][x])) {
                        p.setPointInBoolData(x, y, true);
                    } else {
                        p.setPointInBoolData(x, y, false);
                    }
                } else {
                    // point is outside of cropped area, set it to FALSE
                    p.setPointInBoolData(x, y, false);
                }
            }
        }
    }

    public static void edgeDetection(Pallet p) {

        // detect the edges of the boolean image
        int xRes = p.getImageParameters().getCameraResolution_x();
        int yRes = p.getImageParameters().getCameraResolution_y();
        int edgePixels = 0;
        p.setEdgeImage(new boolean[yRes][xRes]);

        for (int y = 0; y < yRes - 1; y++) {
            for (int x = 0; x < xRes - 1; x++) {

                int edge = Math.abs(p.getBoolImagePointAsInt(x, y) - p.getBoolImagePointAsInt(x+1, y+1))
                            + Math.abs(p.getBoolImagePointAsInt(x+1, y) - p.getBoolImagePointAsInt(x, y+1));

                if (edge > 0) {
                    p.setPointInEdgeImage(x, y, true);
                    edgePixels = edgePixels + 1;
                } else {
                    p.setPointInEdgeImage(x, y, false);
                }

            }
        }
        p.setEdgePixels(edgePixels);
    }

    public static void houghTransform(Pallet p) {

        // setup list of arrays to save hough images in
        List<int[][]> houghLayers = new ArrayList<>();
        // hough transform - find circles in the edge image
        int xRes = p.getImageParameters().getCameraResolution_x();
        int yRes = p.getImageParameters().getCameraResolution_y();
        p.setHoughRunningAccumulator(RadiusAndValue.createTwoDimensionalArray(xRes, yRes));

        for (int r = p.getFromRadiusPixels(); r <= p.getToRadiusPixels(); r++) {

            int[][] houghAccumulator = oneRadiusHoughIteration(p.getEdgeImage(),
                    r,
                    p.getImageParameters().getHoughThetaIncrement(),
                    p.getHoughRunningAccumulator());

            // before moving to the next radius, compare the hough array to the hough accumulator array and update the largest values found
            // updateRunningAccumulator(p.getHoughRunningAccumulator(), houghAccumulator, r);
            // add last hough layer for radius r into the list
            houghLayers.add(houghAccumulator);
            
        }

        // calculate mean and standard deviation of all the hough accumulated values
        p.setHoughMeanAndStdDeviation(calculateMeanAndStd(p.getHoughRunningAccumulator()));

        p.setHoughLayers(houghLayers);
    }

    public static MeanStandardDeviation calculateMeanAndStd(RadiusAndValue[][] rAndV) {
        int yRes = rAndV.length;
        int xRes = rAndV[0].length;
        List<Integer> values = new ArrayList<>();
        for (int y = 0; y < yRes; y++) {
            for (int x = 0; x < xRes; x++) {
                if (rAndV[y][x].getValue() > 0) {
                    values.add(rAndV[y][x].getValue());
                }
            }
        }
        return (new MeanStandardDeviation(values));
    }

    public static void updateRunningAccumulator(RadiusAndValue[][] houghRunningAccumulator, int[][] houghAccumulator, int radius) {

        int yRes = houghAccumulator.length;
        int xRes = houghAccumulator[0].length;

        for (int y = 0; y < yRes; y++) {
            for (int x = 0; x < xRes; x++) {

                int value = houghAccumulator[y][x];
                RadiusAndValue runningHoughPoint = houghRunningAccumulator[y][x];

                if (value > runningHoughPoint.getValue()) {
                    houghRunningAccumulator[y][x].setValue(value);
                    houghRunningAccumulator[y][x].setRadius(radius);
                }

            }
        }
    }

    public static int[][] oneRadiusHoughIteration(boolean[][] edgeImage, int radius, int thetaIncrement, RadiusAndValue[][] runningHoughAccumulator) {

        int yRes = edgeImage.length;
        int xRes = edgeImage[0].length;
        int[][] accumulator = new int[yRes][xRes];

        for (int y = 0; y < yRes; y++) {
            for (int x = 0; x < xRes; x++) {

                if (edgeImage[y][x]) {

                    for (int theta = 0; theta < 360; theta = theta + thetaIncrement) {

                        // xPoint := (r * cos(rad(theta))) + x
                        // yPoint := (r * sin(rad(theta))) + y
                        int xPoint = new BigDecimal(Math.cos(Math.toRadians(theta))).setScale(15,RoundingMode.HALF_UP)
                                            .multiply(new BigDecimal(radius).setScale(15,RoundingMode.HALF_UP))
                                            .add(new BigDecimal(x))
                                            .setScale(0, RoundingMode.HALF_UP).intValue();

                        int yPoint = new BigDecimal(Math.sin(Math.toRadians(theta))).setScale(15,RoundingMode.HALF_UP)
                                .multiply(new BigDecimal(radius).setScale(15,RoundingMode.HALF_UP))
                                .add(new BigDecimal(y))
                                .setScale(0, RoundingMode.HALF_UP).intValue();

                        if (valIsBetween(0, xPoint, xRes -1) && valIsBetween(0, yPoint, yRes -1)) {
                            accumulator[yPoint][xPoint] = accumulator[yPoint][xPoint] + 1;

                            if (accumulator[yPoint][xPoint] > runningHoughAccumulator[yPoint][xPoint].getValue()) {
                                runningHoughAccumulator[yPoint][xPoint].setValue(accumulator[yPoint][xPoint]);
                                runningHoughAccumulator[yPoint][xPoint].setRadius(radius);
                            }

                        }
                    }
                }
            }
        }
        return accumulator;
    }

    public static void findStacks(Pallet p) {

        // Look through the Running Accumulator array (after the Hough transform has run) to find the highest accumulated values
        // The highest values will correspond to the center of the tire stacks
        int stackId = 0;
        int expectedStacks = p.getExpectedStackQty();
        int xRes = p.getImageParameters().getCameraResolution_x();
        int yRes = p.getImageParameters().getCameraResolution_y();

        for (int i = 0; i < expectedStacks; i++) {

            SuspectedStack suspectedStack = new SuspectedStack();
            List<MeasuredStackData> measuredStacks = new ArrayList<>();
            stackId = stackId + 1;

            suspectedStack.setStackId(stackId);

            for (int y = 0; y < yRes; y++) {
                for (int x = 0; x < xRes; x++) {

                    int runningHoughValue = p.getHoughRunningAccumulatorPoint(x, y).getValue();
                    int runningRadiusValue = p.getHoughRunningAccumulatorPoint(x, y).getRadius();
                    int currentHighValue = suspectedStack.getHoughValue();

                    if ((runningHoughValue > currentHighValue) && (runningRadiusValue != 0)) {
                        suspectedStack.setHoughValue(runningHoughValue);
                        suspectedStack.setPixelRadius(runningRadiusValue);
                        suspectedStack.setxPixel(x);
                        suspectedStack.setyPixel(y);
                    }
                }
            }

            // enter a FOR loop to cycle through the stack heights in the HeightArray -- calculate a radius in pixels based on the height being tested in an attempt to sample
            // for tire heights in the raw data array - exit if samples are found
            for (int height: p.getUniqueStackHeights()) {
                // take samples from the raw data array (around the center point found) to find the stack's measured value
                //  - first, convert sample distance to a pixel value based on the calculated distance from camera
                int distanceFromCamera = p.getImageParameters().getFloorDistanceFromCamera() - p.getImageParameters().getPalletHeightFromFloor() - height;
                // (A_x * rRadius_mm) / (StackDistanceFromCamera_mm);
                int samplePointFromCenterMm = p.getSampleDistanceFromCenter();
                BigDecimal Ax = p.getImageParameters().getCameraFactor_x();
                int samplePointFromCenterPixels = Ax.multiply(new BigDecimal(samplePointFromCenterMm))
                                        .divide(new BigDecimal(distanceFromCamera),0,RoundingMode.HALF_UP).intValue();

                int thetaIncrement = p.getImageParameters().getSampleThetaIncrement();
                List<Integer> sampleArray = new ArrayList<>();
                for (int theta = 0; theta < 360; theta = theta + thetaIncrement) {

                    // xPoint := x + (r * cos(rad(theta)))
                    // yPoint := y + (r * sin(rad(theta)))
                    BigDecimal xPoint = new BigDecimal(Math.cos(Math.toRadians(theta))).setScale(15,RoundingMode.HALF_UP);
                    xPoint = new BigDecimal(samplePointFromCenterPixels).multiply(xPoint);
                    int x = suspectedStack.getxPixel();
                    int xPointInt = new BigDecimal(x).add(xPoint).setScale(0,RoundingMode.HALF_UP).intValue();

                    BigDecimal yPoint = new BigDecimal(Math.sin(Math.toRadians(theta))).setScale(15,RoundingMode.HALF_UP);
                    yPoint = new BigDecimal(samplePointFromCenterPixels).multiply(yPoint);
                    int y = suspectedStack.getyPixel();
                    int yPointInt = new BigDecimal(y).add(yPoint).setScale(0,RoundingMode.HALF_UP).intValue();

                    // if sample point is within the image coordinates, and the BoolArray is TRUE for the same coordinate (meaning a distance less than the filtering threshold is at this point) add
                    // the sampled distance value to an array
                    if (valIsBetween(0, xPointInt, xRes-1) && valIsBetween(0, yPointInt, yRes-1)) {
                        boolean boolImagePoint = p.getBoolImage()[yPointInt][xPointInt];
                        if (boolImagePoint) {
                            sampleArray.add(p.getFilteredAndCorrectedImage()[yPointInt][xPointInt]);
                        }
                    }

                }

                // if samples were found, calculate their mean
                if (sampleArray.size() > 0) {

                    MeanStandardDeviation msd = new MeanStandardDeviation(sampleArray);

                    // Error adjustment for height
                    if (p.getImageParameters().isErrorCorrectionHeightAdjustmentEnabled()) {
                        // mx + b
                        BigDecimal m = p.getImageParameters().getErrorCorrectionHeightAdjustment_m();
                        BigDecimal b = p.getImageParameters().getErrorCorrectionHeightAdjustment_b();
                        BigDecimal adjustedMean = m.multiply(msd.getMean()).add(b);
                        msd.setMean(adjustedMean);
                    }

                    MeasuredStackData measuredStack = new MeasuredStackData();
                    measuredStack.setSamples(p.getImageParameters().getTireSamplePoints());
                    measuredStack.setSuccessfulSamples(sampleArray.size());
                    measuredStack.setMeasuredDistanceFromCamera(msd.getMean().setScale(0,RoundingMode.HALF_UP).intValue());
                    measuredStack.setStdDeviation(msd.getStdDeviation());
                    measuredStack.setMean(msd.getMean());
                    measuredStack.setMeasuredStackHeight(p.getImageParameters().getFloorDistanceFromCamera()
                                                            - p.getImageParameters().getPalletHeightFromFloor()
                                                            - measuredStack.getMeasuredDistanceFromCamera());

                    measuredStacks.add(measuredStack);
                }
            }

            if (measuredStacks.size() > 0) {
                // pick sampled data with the highest success rate
                Collections.sort(measuredStacks, (s1, s2) -> (s2.getSuccessfulSamples() - s1.getSuccessfulSamples()));
                MeasuredStackData bestStack = measuredStacks.get(0);
                suspectedStack.mergeMeasuredData(bestStack);
            }
            p.getSuspectedStacks().add(suspectedStack);

            // determine coordinates for a square around the center point - these will be zero'd
            int xStart = suspectedStack.getxPixel() - (suspectedStack.getPixelRadius() / 2);
            int xEnd = suspectedStack.getxPixel() + (suspectedStack.getPixelRadius() / 2);
            int yStart = suspectedStack.getyPixel() - (suspectedStack.getPixelRadius() / 2);
            int yEnd = suspectedStack.getyPixel() + (suspectedStack.getPixelRadius() / 2);

            // limit coordinates above to inside the image resolution
            xStart = limitInt(0, xStart, xRes - 1);
            xEnd = limitInt(0, xEnd, xRes - 1);
            yStart = limitInt(0, yStart, yRes - 1);
            yEnd = limitInt(0, yEnd, yRes - 1);

            // zero the area around the highest value found
            for (int y = yStart; y <= yEnd; y++) {
                for (int x = xStart; x <= xEnd; x++) {
                    p.getHoughRunningAccumulator()[y][x].setValue(0);
                    p.getHoughRunningAccumulator()[y][x].setRadius(0);
                }
            }
        }
    }

    public static void matchStacks(Pallet p) {

        p.setUnmatchedStacks(new ArrayList<>());

        // arrange the suspected, and expected stack lists from tallest to shortest
        if (p.getSuspectedStacks().size() > 1) {
            Collections.sort(p.getSuspectedStacks(), (s1, s2) -> (s2.getMeasuredHeight() - s1.getMeasuredHeight()));
        }
        if (p.getExpectedStacks().size() > 1) {
            Collections.sort(p.getExpectedStacks(), (s1, s2) -> (s2.getExpectedHeight() - s1.getExpectedHeight()));
        }

        // pull tires from the suspected tire list, and check them against the ordered (by expected height) expected tire list
        // - make sure they're within the height tolerance
        int iterations = p.getSuspectedStacks().size();
        if (p.getExpectedStacks().size() == p.getSuspectedStacks().size() && (p.getExpectedStacks().size() > 0)) {
            for (int i = 0; i < iterations; i++) {

                SuspectedStack suspectedStack = p.getSuspectedStacks().remove(0);
                Stack expectedStack = p.getExpectedStacks().get(i);
                int deviation = p.getSwDeviation().setScale(0, RoundingMode.HALF_UP).intValue();
                int acceptedSampleSuccessRate = p.getImageParameters().getAcceptedSampleSuccessPercent();
                int suspectedStackSampleSuccessRate = suspectedStack.getSampleSuccessRate().setScale(0, RoundingMode.HALF_UP).intValue();

                if ((areStacksWithinDeviation(expectedStack, suspectedStack, deviation))
                        && (suspectedStackSampleSuccessRate >= acceptedSampleSuccessRate)) {


                    expectedStack.setStackMatched(true);
                    expectedStack.setFromSuspectedStack(suspectedStack);
                    expectedStack.setxPixel(suspectedStack.getxPixel());
                    expectedStack.setyPixel(suspectedStack.getyPixel());
                    expectedStack.setMeasuredHeight(suspectedStack.getMeasuredHeight());

                } else {

                    expectedStack.setStackMatched(false);
                    p.getUnmatchedStacks().add(suspectedStack);

                }
            }
        }
    }

    public static void calculateRealStackPositions(Pallet p) {

        int xRes = p.getImageParameters().getCameraResolution_x();
        int yRes = p.getImageParameters().getCameraResolution_y();

        List<Stack> expectedStacks = p.getExpectedStacks();
        for (Stack expectedStack: expectedStacks) {

            if (expectedStack.isStackMatched()) {

                BigDecimal distanceFromCenterPixels_x = new BigDecimal(expectedStack.getxPixel() - (xRes / 2))
                                                        .setScale(15,RoundingMode.HALF_UP);
                BigDecimal distanceFromCenterPixels_y = new BigDecimal((yRes / 2) - expectedStack.getyPixel())
                                                        .setScale(15, RoundingMode.HALF_UP);
                BigDecimal stackDistanceFromCamera = expectedStack.getFromSuspectedStack().getMsd().getMean();
                BigDecimal Ax = p.getImageParameters().getCameraFactor_x().setScale(15,RoundingMode.HALF_UP);
                BigDecimal Ay = p.getImageParameters().getCameraFactor_y().setScale(15,RoundingMode.HALF_UP);

                BigDecimal distanceFromCenterMm_x = distanceFromCenterPixels_x
                                                        .multiply(stackDistanceFromCamera)
                                                        .divide(Ax,15,RoundingMode.HALF_UP);
                BigDecimal distanceFromCenterMm_y = distanceFromCenterPixels_y
                                                        .multiply(stackDistanceFromCamera)
                                                        .divide(Ay,15,RoundingMode.HALF_UP);

                int distanceFromCenterMmInt_x = distanceFromCenterMm_x.setScale(0,RoundingMode.HALF_UP).intValue();
                int distanceFromCenterMmInt_y = distanceFromCenterMm_y.setScale(0,RoundingMode.HALF_UP).intValue();

                BigDecimal distanceFromCenterMmCorrected_x = BigDecimal.ZERO;
                BigDecimal distanceFromCenterMmCorrected_y = BigDecimal.ZERO;

                expectedStack.setxDistanceFromCenter_mm(distanceFromCenterMm_x);
                expectedStack.setyDistanceFromCenter_mm(distanceFromCenterMm_y);

                // adjust for stack skew in image
                if (p.getImageParameters().isErrorCorrectionSkewAdjustmentEnabled()) {

                    // quadrant 1
                    if ((distanceFromCenterMmInt_x >= 0) && (distanceFromCenterMmInt_y >= 0)) {
                        distanceFromCenterMmCorrected_x = p.getImageParameters().getErrorCorrectionQuadrant1xCoefficients()
                                .calculateForX(distanceFromCenterMm_x);
                        distanceFromCenterMmCorrected_y = p.getImageParameters().getErrorCorrectionQuadrant1yCoefficients()
                                .calculateForX(distanceFromCenterMm_y);
                    }
                    // quadrant 2
                    else if ((distanceFromCenterMmInt_x < 0) && (distanceFromCenterMmInt_y >= 0)) {
                        distanceFromCenterMmCorrected_x = p.getImageParameters().getErrorCorrectionQuadrant2xCoefficients()
                                .calculateForX(distanceFromCenterMm_x);
                        distanceFromCenterMmCorrected_y = p.getImageParameters().getErrorCorrectionQuadrant2yCoefficients()
                                .calculateForX(distanceFromCenterMm_y);
                    }
                    // quadrant 3
                    else if ((distanceFromCenterMmInt_x < 0) && (distanceFromCenterMmInt_y < 0)) {
                        distanceFromCenterMmCorrected_x = p.getImageParameters().getErrorCorrectionQuadrant3xCoefficients()
                                .calculateForX(distanceFromCenterMm_x);
                        distanceFromCenterMmCorrected_y = p.getImageParameters().getErrorCorrectionQuadrant3yCoefficients()
                                .calculateForX(distanceFromCenterMm_y);
                    }
                    // quadrant 4
                    else if ((distanceFromCenterMmInt_x >= 0) && (distanceFromCenterMmInt_y < 0)) {
                        distanceFromCenterMmCorrected_x = p.getImageParameters().getErrorCorrectionQuadrant4xCoefficients()
                                .calculateForX(distanceFromCenterMm_x);
                        distanceFromCenterMmCorrected_y = p.getImageParameters().getErrorCorrectionQuadrant4yCoefficients()
                                .calculateForX(distanceFromCenterMm_y);
                    }
                }

                // calculate stack coordinates from pallet origin
                int xDistance = 0;
                int yDistance = 0;
                int xCenterDistanceFromOrigin = p.getImageParameters().getDistanceToCenterOfFrameFromPalletOriginMM_x();
                int yCenterDistanceFromOrigin = p.getImageParameters().getDistanceToCenterOfFrameFromPalletOriginMM_y();

                if (p.getImageParameters().isErrorCorrectionSkewAdjustmentEnabled()) {
                    expectedStack.setxDistanceFromCenterAdjusted_mm(distanceFromCenterMmCorrected_x);
                    expectedStack.setyDistanceFromCenterAdjusted_mm(distanceFromCenterMmCorrected_y);
                    xDistance = distanceFromCenterMmCorrected_x
                            .add(new BigDecimal(xCenterDistanceFromOrigin))
                            .setScale(0,RoundingMode.HALF_UP)
                            .intValue();
                    yDistance = distanceFromCenterMmCorrected_y
                            .add(new BigDecimal(yCenterDistanceFromOrigin))
                            .setScale(0,RoundingMode.HALF_UP)
                            .intValue();
                } else {
                    xDistance = distanceFromCenterMm_x
                            .add(new BigDecimal(xCenterDistanceFromOrigin))
                            .setScale(0,RoundingMode.HALF_UP)
                            .intValue();
                    yDistance = distanceFromCenterMm_y
                            .add(new BigDecimal(yCenterDistanceFromOrigin))
                            .setScale(0,RoundingMode.HALF_UP)
                            .intValue();
                }

                expectedStack.setxDistanceFromPalletOrigin_mm(xDistance);
                expectedStack.setyDistanceFromPalletOrigin_mm(yDistance);

            }
        }
    }

    // static methods
    private static int limitInt(int min, int value, int max) {
        int r = 0;
        if ((value >= min) && (value <= max)) {
            r = value;
        } else if (value < min) {
            r = min;
        } else if (value > max) {
            r = max;
        }
        return r;
    }

    private static boolean valIsBetween(int min, int val, int max) {
        if ((val >= min) && (val <= max)) {
            return true;
        } else {
            return false;
        }
    }

    private static boolean areStacksWithinDeviation(Stack expectedStack, SuspectedStack suspectedStack, int deviation) {

        int upperLimit = expectedStack.getExpectedHeight() + deviation;
        int lowerLimit = expectedStack.getExpectedHeight() - deviation;
        int value = suspectedStack.getMeasuredHeight();
        boolean result = valIsBetween(lowerLimit, value, upperLimit);
        return result;

    }

    public static void parallelHoughTransform(Pallet p, int threads) throws InterruptedException {

        Message<HoughMessage> msg = new Message<>();
        ExecutorService es = Executors.newFixedThreadPool(threads);

        int radiusFrom = p.getFromRadiusPixels();
        int radiusTo = p.getToRadiusPixels();
        int thetaIncrement = p.getImageParameters().getHoughThetaIncrement();
        int xRes = p.getImageParameters().getCameraResolution_x();
        int yRes = p.getImageParameters().getCameraResolution_y();
        RadiusAndValue[][] runningHoughAccumulator = RadiusAndValue.createTwoDimensionalArray(xRes, yRes);

        for (int radius = radiusFrom; radius <=radiusTo; radius++) {
            boolean[][] edgeArray = Clone.deepClone(p.getEdgeImage());
            es.execute(new HoughWorkerThread(radius, thetaIncrement, edgeArray, msg, runningHoughAccumulator));
        }
        es.shutdown();
        es.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);

        List<HoughMessage> houghArrays = msg.removeAll();
        List<int[][]> arrays = new ArrayList<>();
        for (HoughMessage hm: houghArrays) {
            arrays.add(hm.getHoughArray());
        }
        MeanStandardDeviation msd = ImageProcessing.calculateMeanAndStd(runningHoughAccumulator);

        p.setHoughMeanAndStdDeviation(msd);
        p.setHoughRunningAccumulator(runningHoughAccumulator);
        p.setHoughLayers(arrays);

    }

    public static void postDetection(Pallet p) {

        int upperSampleLimit = p.getImageParameters().getFloorDistanceFromCamera() -
                (p.getImageParameters().getPalletHeightFromFloor() + p.getImageParameters().getPostHeight()
                + p.getImageParameters().getPostHeightDeviation());
        int lowerSampleLimit = p.getImageParameters().getFloorDistanceFromCamera() -
                (p.getImageParameters().getPalletHeightFromFloor() + p.getImageParameters().getPostHeight()
                - p.getImageParameters().getPostHeightDeviation());
        BigDecimal testSampleRate = new BigDecimal(p.getImageParameters().getPostSampleSuccessRate());

        for (Square s: p.getImageParameters().getPostAreas()) {

            List<Integer> samples = new ArrayList<>();
            int start_x = s.getTopLeftBoundary_x();
            int end_x = s.getBottomRightBoundary_x();
            int start_y = s.getTopLeftBoundary_y();
            int end_y = s.getBottomRightBoundary_y();

            for (int y = start_y; y <= end_y; y++) {
                for (int x = start_x; x <= end_x; x++) {

                    int point = p.getOriginalImage()[y][x];
                    if (valIsBetween(upperSampleLimit,point,lowerSampleLimit)) {
                        samples.add(point);
                    }
                }
            }

            MeanStandardDeviation msd = null;
            if (samples.size() > 1) {
                msd = new MeanStandardDeviation(samples);
                BigDecimal sampleSuccessRate = new BigDecimal(samples.size()).setScale(2,BigDecimal.ROUND_HALF_UP)
                        .divide(new BigDecimal(s.getPixels()),4, BigDecimal.ROUND_HALF_UP)
                        .multiply(new BigDecimal(100)).setScale(2,BigDecimal.ROUND_HALF_UP);

                // a post was found
                if (BigDecimalMath.GEQ(sampleSuccessRate,testSampleRate)) {

                    int postHeight = p.getImageParameters().getFloorDistanceFromCamera()
                            - p.getImageParameters().getPalletHeightFromFloor()
                            - msd.getMean().intValue();

                    Post foundPost = new Post(s,msd,samples.size(),sampleSuccessRate, postHeight);
                    p.getDetectedPosts().add(foundPost);
                }
            }
        }

    }

}
