package com.cimcorp.plc.util;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class PalletBitmap {

    private static final int STARTING_COLOUR = 0;
    private static final int LARGEST_COLOUR = 255;
    private static final int GREEN = 0x2BFF00;
    private static final int RED = 0xFF2A00;

    static void mergeLayersAndCreateBitmap(List<int[][]> layers, String filename) {

        int height = layers.get(0).length;
        int width = layers.get(0)[0].length;

        int[][] mergedLayers = new int[height][width];

        for (int[][] layer: layers) {
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    mergedLayers[y][x] = mergedLayers[y][x] + layer[y][x];
                }
            }
        }
        createBitmap(mergedLayers, filename, false);
    }

    static void createBitmap(boolean[][] data, String filename) {

        int height = data.length;
        int width = data[0].length;

        int[][] internalData = new int[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (data[y][x]) {
                    internalData[y][x] = 255;
                } else {
                    internalData[y][x] = 0;
                }
            }
        }

        createBitmap(internalData, filename, true);
    }


    public static void createBitmap(int[][] data, String filename, boolean monochrome) {

        BufferedImage img = getBufferedImage(data, monochrome);

        writeToDisk(filename, img);

    }

    private static BufferedImage getBufferedImage(int[][] data, boolean monochrome) {
        int height = data.length;
        int width = data[0].length;
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        int[][] scaledData = scaleImageData(data, monochrome, height, width);

        int[] flattenedData = flattenImageData(height, width, scaledData);

        img.getRaster().setPixels(0, 0, width, height, flattenedData);
        return img;
    }

    private static int[] flattenImageData(int height, int width, int[][] scaledData) {
        int[] flattenedData = new int[width * height *3];
        int ind = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int greyShade = LARGEST_COLOUR - scaledData[y][x];
                flattenedData[ind] = greyShade;
                flattenedData[ind+1] = greyShade;
                flattenedData[ind+2] = greyShade;
                ind = ind + 3;
            }
        }
        return flattenedData;
    }

    private static int[][] scaleImageData(int[][] data, boolean monochrome, int height, int width) {
        // find the highest and lowest values in the input array
        int maxValue = -2147483648;
        int smallestValue = 2147483647;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (data[y][x] > maxValue) {
                    maxValue = data[y][x];
                }
                if (data[y][x] < smallestValue) {
                    smallestValue = data[y][x];
                }
            }
        }
        // scale the input values from 0 - 255
        int minValue;
        if (monochrome) {
            minValue = 0;
        } else {
            minValue = STARTING_COLOUR;
        }
        int[][] internalData = new int[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                internalData[y][x] = scaleInt(smallestValue, maxValue, data[y][x], minValue, LARGEST_COLOUR);
            }
        }
        return internalData;
    }

    private static void writeToDisk(String filename, BufferedImage img) {
        // write to disk
        String path = Paths.get(".").toAbsolutePath().normalize().toString() + "\\";
        Path p = Paths.get(path);
        if (!Files.exists(p)) {
            try {
                Files.createDirectories(p);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        File file = new File(path + filename + ".png");
        try {
            file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (file.exists()) {
            if (file.canWrite()) {

                try {
                    ImageIO.write(img, "png", file);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static int scaleInt(int inputMin, int inputMax, int valueToScale, int outputMin, int outputMax) {

        int inputRange = inputMax - inputMin;
        int inputOffset = 0 - inputMin;
        int internalValueToScale = valueToScale + inputOffset;

        int outputRange = outputMax - outputMin;
        int outputOffset = 0 - outputMin;

        if (inputRange == 0) {
            return outputMin;
        } else if (inputMin == valueToScale) {
            return 0;
        }

        int scaledVal = new BigDecimal(internalValueToScale).setScale(10, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal(outputRange)).setScale(10, RoundingMode.HALF_UP)
                    .divide(new BigDecimal(inputRange).setScale(10,RoundingMode.HALF_UP),10, RoundingMode.HALF_UP)
                    .subtract(new BigDecimal(outputOffset)).setScale(0, RoundingMode.HALF_UP)
                    .intValue();

        return scaledVal;
    }

    public static void drawHoughCirclesOnOriginal(Pallet p, String filename) {

        int[][] originalImage = Clone.deepClone(p.getOriginalImage());
        BufferedImage img = getBufferedImage(originalImage,false);

        int yRes = originalImage.length;
        int xRes = originalImage[0].length;

        // draw the matched stacks
        for (Stack stack: p.getExpectedStacks()) {

            if (stack.isStackMatched()) {

                int drawingColour = GREEN;
                int radius = stack.getFromSuspectedStack().getPixelRadius();
                int xMiddleOfCircle = stack.getxPixel();
                int yMiddleOfCircle = stack.getyPixel();

                drawCircleOnImage(img, drawingColour, radius, xMiddleOfCircle, yMiddleOfCircle);

            }
        }

        // draw the unmatched stacks
        for (SuspectedStack stack: p.getUnmatchedStacks()) {

            int drawingColour = RED;
            int radius = stack.getPixelRadius();
            int xMiddleOfCircle = stack.getxPixel();
            int yMiddleOfCircle = stack.getyPixel();

            drawCircleOnImage(img, drawingColour, radius, xMiddleOfCircle, yMiddleOfCircle);

        }

        writeToDisk(filename, img);

    }

    private static void drawCircleOnImage(BufferedImage img, int drawingColour, int radius, int xMiddleOfCircle, int yMiddleOfCircle) {
        img.setRGB(xMiddleOfCircle, yMiddleOfCircle, drawingColour);
        for (int theta = 0; theta < 360; theta++){

            int xPoint = new BigDecimal(Math.cos(Math.toRadians(theta))).setScale(15,RoundingMode.HALF_UP)
                    .multiply(new BigDecimal(radius).setScale(15,RoundingMode.HALF_UP))
                    .add(new BigDecimal(xMiddleOfCircle))
                    .setScale(0, RoundingMode.HALF_UP).intValue();

            int yPoint = new BigDecimal(Math.sin(Math.toRadians(theta))).setScale(15,RoundingMode.HALF_UP)
                    .multiply(new BigDecimal(radius).setScale(15,RoundingMode.HALF_UP))
                    .add(new BigDecimal(yMiddleOfCircle))
                    .setScale(0, RoundingMode.HALF_UP).intValue();

            img.setRGB(xPoint,yPoint, drawingColour);
        }
    }

}
