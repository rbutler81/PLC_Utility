package com.cimcorp.plc.util;

import configFileUtil.BD;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Pallet {

    private int msg;
    private int trackingNumber;
    private int od;
    private int id;
    private int sw;
    private int expectedStackQty;
    private boolean allStacksSameHeight;
    private List<Stack> expectedStacks = new ArrayList<>();
    private List<SuspectedStack> suspectedStacks= new ArrayList<>();
    private List<SuspectedStack> unmatchedStacks = new ArrayList<>();
    // algorithm parameters
    private ImageParameters ip;
    private BigDecimal odRadius;
    private BigDecimal swDeviation;
    private int fromRadiusPixels;
    private int toRadiusPixels;
    private int heightThresholdMin;
    private int heightThresholdMax;
    private int sampleDistanceFromCenter;
    private int edgePixels = 0;
    private List<Integer> uniqueStackHeights = new ArrayList<>();
    private MeanStandardDeviation houghMeanAndStdDeviation = new MeanStandardDeviation();
    // Images
    private int[][] originalImage;
    private int[][] filteredAndCorrectedImage;
    private boolean[][] boolImage;
    private boolean[][] edgeImage;
    private int[][] houghAccumulator;
    private List<int[][]> houghLayers;
    private RadiusAndValue[][] houghRunningAccumulator;

    public Pallet(String s) throws KeyValuePairException, PalletException {

        List<KeyValuePair> kvp = KeyValuePair.keyValuePairsFromStringList(s);
        int stackId = 1;

        for (KeyValuePair k: kvp) {

            if (k.getKey().toLowerCase().equals("msg")) {
                msg = k.getValueAsInteger();
            } else if (k.getKey().toLowerCase().equals("tn")) {
                trackingNumber = k.getValueAsInteger();
            } else if (k.getKey().toLowerCase().equals("od")) {
                od = k.getValueAsInteger();
            } else if (k.getKey().toLowerCase().equals("id")) {
                id = k.getValueAsInteger();
            } else if (k.getKey().toLowerCase().equals("sw")) {
                sw = k.getValueAsInteger();
            } else if (k.getKey().toLowerCase().equals("n1") ||
                    k.getKey().toLowerCase().equals("n2") ||
                    k.getKey().toLowerCase().equals("n3") ||
                    k.getKey().toLowerCase().equals("n4") ||
                    k.getKey().toLowerCase().equals("n5")) {
                if (k.getValueAsInteger() > 0) {
                    expectedStacks.add(new Stack(stackId, k.getValueAsInteger()));
                    stackId = stackId + 1;
                }
            } else {
                throw new KeyValuePairException(k);
            }
        }

        expectedStackQty = expectedStacks.size();
        if ((sw > 0) && (expectedStackQty > 0)) {
            allStacksSameHeight = true;
            int stackQtyToMatch = expectedStacks.get(0).getTireQty();
            for (Stack stack: expectedStacks) {
                if (allStacksSameHeight) {
                    allStacksSameHeight = (stackQtyToMatch == stack.getTireQty());
                }
                stack.setExpectedHeight(sw);
            }
        }

        if ((msg > 0) && (trackingNumber > 0) && (od > 0) && (id > 0) && (sw > 0) && (expectedStackQty > 0)) {
        } else {
            throw new PalletException(s);
        }
    }

    public void setupDetectionParameters(ImageParameters ip) {

        this.ip = ip;

        odRadius = BD.divide(od,2,1);
        swDeviation = BD.multiply(sw, ip.getSearchForRadiusDeviation());
        sampleDistanceFromCenter = ((od / 2) + (id / 2)) / 2;

        if (allStacksSameHeight) {

            int stackDistanceFromCamera = ip.getFloorDistanceFromCamera() - ip.getPalletHeightFromFloor()
                                            - expectedStacks.get(0).getExpectedHeight();

            // (A_x * rRadius_mm) / (rStackDistanceFromCamera_mm + rDeviation_mm);
            BigDecimal numerator = BD.multiply(odRadius, ip.getCameraFactor_x());
            BigDecimal denominator = BD.add(stackDistanceFromCamera, swDeviation);
            fromRadiusPixels = BD.divide(numerator, denominator, 0) - ip.getToFromPixelSearchAdjust();
            // (A_x * rRadius_mm) / (rStackDistanceFromCamera_mm - rDeviation_mm);
            numerator = BD.multiply(odRadius, ip.getCameraFactor_x());
            denominator = BD.subtract(stackDistanceFromCamera, swDeviation);
            toRadiusPixels = BD.divide(numerator, denominator, 0) + ip.getToFromPixelSearchAdjust();

            // calculate heights to filter out of the 3d image data
            heightThresholdMin = BD.subtract(stackDistanceFromCamera, swDeviation, 0);
            heightThresholdMax = BD.add(stackDistanceFromCamera, swDeviation, 0);

        } else {

            // order stack list by height -- tallest first
            Collections.sort(expectedStacks, (s1, s2) -> (s2.getTireQty() - s1.getTireQty()));

            int stackDistanceFromCameraMax = ip.getFloorDistanceFromCamera() - ip.getPalletHeightFromFloor()
                    - expectedStacks.get(expectedStackQty - 1).getExpectedHeight();
            int stackDistanceFromCameraMin = ip.getFloorDistanceFromCamera() - ip.getPalletHeightFromFloor()
                    - expectedStacks.get(0).getExpectedHeight();

            // (A_x * rRadius_mm) / (rStackDistanceFromCamera_mm + rDeviation_mm);
            BigDecimal numerator = BD.multiply(odRadius, ip.getCameraFactor_x());
            BigDecimal denominator = BD.add(stackDistanceFromCameraMax, swDeviation);
            fromRadiusPixels = BD.divide(numerator, denominator, 0) - ip.getToFromPixelSearchAdjust();
            // (A_x * rRadius_mm) / (rStackDistanceFromCamera_mm - rDeviation_mm);
            numerator = BD.multiply(odRadius, ip.getCameraFactor_x());
            denominator = BD.subtract(stackDistanceFromCameraMin, swDeviation);
            toRadiusPixels = BD.divide(numerator, denominator, 0) + ip.getToFromPixelSearchAdjust();

            // calculate heights to filter out of the 3d image data
            heightThresholdMin = BD.subtract(stackDistanceFromCameraMin, swDeviation, 0);
            heightThresholdMax = BD.add(stackDistanceFromCameraMax, swDeviation, 0);

        }

        for (Stack stack: expectedStacks) {
            if (!uniqueStackHeights.contains(stack.getExpectedHeight())) {
                uniqueStackHeights.add(stack.getExpectedHeight());
            }
        }
    }

    public void setFilteredAndCorrectedImage(int[][] filteredAndCorrectedImage) {
        this.filteredAndCorrectedImage = filteredAndCorrectedImage;
    }

    public int getMsg() {
        return msg;
    }

    public int getTrackingNumber() {
        return trackingNumber;
    }

    public int getOd() {
        return od;
    }

    public int getId() {
        return id;
    }

    public int getSw() {
        return sw;
    }

    public int getExpectedStackQty() {
        return expectedStackQty;
    }

    public boolean isAllStacksSameHeight() {
        return allStacksSameHeight;
    }

    public List<Stack> getExpectedStacks() {
        return expectedStacks;
    }

    public ImageParameters getIp() {
        return ip;
    }

    public BigDecimal getOdRadius() {
        return odRadius;
    }

    public BigDecimal getSwDeviation() {
        return swDeviation;
    }

    public int getFromRadiusPixels() {
        return fromRadiusPixels;
    }

    public int getToRadiusPixels() {
        return toRadiusPixels;
    }

    public int getHeightThresholdMin() {
        return heightThresholdMin;
    }

    public int getHeightThresholdMax() {
        return heightThresholdMax;
    }

    public List<Integer> getUniqueStackHeights() {
        return uniqueStackHeights;
    }

    public int[][] getFilteredAndCorrectedImage() {
        return filteredAndCorrectedImage;
    }

    public Pallet setHeightThresholdMin(int heightThresholdMin) {
        this.heightThresholdMin = heightThresholdMin;
        return this;
    }

    public Pallet setHeightThresholdMax(int heightThresholdMax) {
        this.heightThresholdMax = heightThresholdMax;
        return this;
    }

    public Pallet setBoolImage(boolean[][] boolImage) {
        this.boolImage = boolImage;
        return this;
    }

    public Pallet setValueInFilteredAndCorrectedImage(int x, int y, int value) {
        filteredAndCorrectedImage[y][x] = value;
        return this;
    }

    public Pallet setPointInBoolData(int x, int y, boolean value) {
        boolImage[y][x] = value;
        return this;
    }

    public Pallet setPointInEdgeImage(int x, int y, boolean value) {
        edgeImage[y][x] = value;
        return this;
    }

    public int[][] getOriginalImage() {
        return originalImage;
    }

    public boolean[][] getBoolImage() {
        return boolImage;
    }

    public Pallet setOriginalImage(int[][] originalImage) {
        this.originalImage = originalImage;
        return this;
    }

    public boolean[][] getEdgeImage() {
        return edgeImage;
    }

    public Pallet setEdgeImage(boolean[][] edgeImage) {
        this.edgeImage = edgeImage;
        return this;
    }

    public int getBoolImagePointAsInt(int x, int y) {
        if (boolImage[y][x]) {
            return 1;
        } else {
            return 0;
        }
    }

    public boolean getEdgeImagePoint(int x, int y) {
        return edgeImage[y][x];
    }

    public int[][] getHoughAccumulator() {
        return houghAccumulator;
    }

    public int getHoughAccumulatorPoint(int x, int y) {
        return houghAccumulator[y][x];
    }

    public Pallet setHoughAccumulatorPoint(int x, int y, int value) {
        houghAccumulator[y][x] = value;
        return this;
    }

    public RadiusAndValue[][] getHoughRunningAccumulator() {
        return houghRunningAccumulator;
    }

    public RadiusAndValue getHoughRunningAccumulatorPoint(int x, int y) {
        return houghRunningAccumulator[y][x];
    }

    public Pallet setHoughAccumulator(int[][] houghAccumulator) {
        this.houghAccumulator = houghAccumulator;
        return this;
    }

    public Pallet setHoughRunningAccumulator(RadiusAndValue[][] houghRunningAccumulator) {
        this.houghRunningAccumulator = houghRunningAccumulator;
        return this;
    }

    public Pallet setHoughRunningAccumulatorPoint(int x, int y, int value, int radius) {
        houghRunningAccumulator[y][x].setValue(value);
        houghRunningAccumulator[y][x].setRadius(radius);
        return this;
    }

    public List<SuspectedStack> getSuspectedStacks() {
        return suspectedStacks;
    }

    public List<SuspectedStack> getUnmatchedStacks() {
        return unmatchedStacks;
    }

    public int getSampleDistanceFromCenter() {
        return sampleDistanceFromCenter;
    }

    public MeanStandardDeviation getHoughMeanAndStdDeviation() {
        return houghMeanAndStdDeviation;
    }

    public Pallet setHoughMeanAndStdDeviation(MeanStandardDeviation houghMeanAndStdDeviation) {
        this.houghMeanAndStdDeviation = houghMeanAndStdDeviation;
        return this;
    }

    public Pallet setUnmatchedStacks(List<SuspectedStack> unmatchedStacks) {
        this.unmatchedStacks = unmatchedStacks;
        return this;
    }

    public List<int[][]> getHoughLayers() {
        return houghLayers;
    }

    public Pallet setHoughLayers(List<int[][]> houghLayers) {
        this.houghLayers = houghLayers;
        return this;
    }

    public int getEdgePixels() {
        return edgePixels;
    }

    public Pallet setEdgePixels(int edgePixels) {
        this.edgePixels = edgePixels;
        return this;
    }
}