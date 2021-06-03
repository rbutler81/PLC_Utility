package com.cimcorp.plc.util.palletImaging;

import com.cimcorp.misc.helpers.Clone;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class PalletDataFiles {

    private static final int STARTING_COLOUR = 0;
    private static final int LARGEST_COLOUR = 255;
    private static final int BLUE = 0x1919FF;
    private static final int RED = 0xFF2A00;

    private int imagesToKeep;
    private String path;
    private int trackingNumber = -1;

    public PalletDataFiles(int imagesToKeep, String path, int trackingNumber) {
        this.imagesToKeep = imagesToKeep;
        this.path = path;
        this.trackingNumber = trackingNumber;
    }

    public PalletDataFiles(int imagesToKeep, String path) {
        this.imagesToKeep = imagesToKeep;
        this.path = path;
    }

    public void mergeLayersAndCreateBitmap(List<int[][]> layers, String filename) throws IOException {

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

    public void createBitmap(boolean[][] data, String filename) throws IOException {

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


    public void createBitmap(int[][] data, String filename, boolean monochrome) throws IOException {

        BufferedImage img = getBufferedImage(data, monochrome);

        writeImageToDisk(filename, img);

    }

    private BufferedImage getBufferedImage(int[][] data, boolean monochrome) {
        int height = data.length;
        int width = data[0].length;
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        int[][] scaledData = scaleImageData(data, monochrome, height, width);

        int[] flattenedData = flattenImageData(height, width, scaledData);

        img.getRaster().setPixels(0, 0, width, height, flattenedData);
        return img;
    }

    private int[] flattenImageData(int height, int width, int[][] scaledData) {
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

    private int[][] scaleImageData(int[][] data, boolean monochrome, int height, int width) {
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

    public void writeImageToDisk(String filename, BufferedImage img) throws IOException {

        // write to disk
        Path p = Paths.get(path);
        writeImageToDiskWithPath(filename, img, p);

    }

    public void writeImageToDiskWithPath(String filename, BufferedImage img, Path p) throws IOException {

        if (!Files.exists(p)) {
            Files.createDirectories(p);
        }

        File file = null;
        if (trackingNumber >= 1) {
            file = getFile(filename, "png");
        } else {
            String s = p.toString() + "\\" + filename + ".png";
            file = new File(s);
        }

        file.createNewFile();

        if (file.exists()) {
            if (file.canWrite()) {
                ImageIO.write(img, "png", file);
            }
        }

        cleanUpFiles(p);
    }

    private void cleanUpFiles(Path p) {
        // make a list of files contained in the images folder
        File[] files = new File(p.toString()).listFiles();
        List<File> listOfFiles = new ArrayList<>();
        for (int i = 0; i < files.length; i++) {
            listOfFiles.add(files[i]);
        }

        // check if the number of files on the disk is greater than the configured amount - if so, delete the oldest ones first
        if (listOfFiles.size() > imagesToKeep) {

            File[] filesToDelete = new File[listOfFiles.size()];
            listOfFiles.toArray(filesToDelete);
            Arrays.sort(filesToDelete, Comparator.comparingLong(File::lastModified));
            int numberOfFilesToDelete = listOfFiles.size() - imagesToKeep;
            for (int i = 0; i < numberOfFilesToDelete; i++) {
                filesToDelete[i].delete();
            }
        }
    }

    private File getFile(String filename, String extension) {

        String originalFileName = filename;
        filename = filename + "_" + trackingNumber + "." + extension;
        File folder = new File(path);
        String[] files = folder.list();

        boolean done = false;
        int append = 1;
        while (!done) {
            boolean exists = false;
            for (String s: files) {
                if (s.equals(filename)) {
                    exists = true;
                }
            }
            if (exists) {
                filename = originalFileName + "_" + trackingNumber + "_" + append + "." + extension;
                append = append + 1;
            } else {
                done = true;
            }

        }
        return new File(path + filename);
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

    public void drawHoughCirclesOnOriginal(Pallet p, String filename) throws IOException {

        int[][] originalImage = Clone.deepClone(p.getOriginalImage());
        BufferedImage img = getBufferedImage(originalImage,false);

        int yRes = originalImage.length;
        int xRes = originalImage[0].length;

        // draw the matched stacks
        for (Stack stack: p.getExpectedStacks()) {

            if (stack.isStackMatched()) {

                int drawingColour = BLUE;
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

        writeImageToDisk(filename, img);

    }

    private static void drawCircleOnImage(BufferedImage img, int drawingColour, int radius, int xMiddleOfCircle, int yMiddleOfCircle) {

        if ((xMiddleOfCircle >= 0) && (xMiddleOfCircle < img.getRaster().getWidth())
                && (yMiddleOfCircle >= 0) && (yMiddleOfCircle < img.getRaster().getHeight())) {
            img.setRGB(xMiddleOfCircle, yMiddleOfCircle, drawingColour);
            for (int theta = 0; theta < 360; theta++) {

                int xPoint = new BigDecimal(Math.cos(Math.toRadians(theta))).setScale(15, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal(radius).setScale(15, RoundingMode.HALF_UP))
                        .add(new BigDecimal(xMiddleOfCircle))
                        .setScale(0, RoundingMode.HALF_UP).intValue();

                int yPoint = new BigDecimal(Math.sin(Math.toRadians(theta))).setScale(15, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal(radius).setScale(15, RoundingMode.HALF_UP))
                        .add(new BigDecimal(yMiddleOfCircle))
                        .setScale(0, RoundingMode.HALF_UP).intValue();


                if ((xPoint >= 0) && (xPoint < img.getRaster().getWidth()) && (yPoint >= 0) && (yPoint < img.getRaster().getHeight())) {
                    img.setRGB(xPoint, yPoint, drawingColour);
                }
            }
        }
    }

    public void saveSerializedData(SerializedPalletDetails data) throws IOException {

        Path p = Paths.get(path);

        if (!Files.exists(p)) {
            Files.createDirectories(p);
        }

        File dataFile = getFile("Data", "ser");

        FileOutputStream fileOut = new FileOutputStream(dataFile.toString());
        ObjectOutputStream out = new ObjectOutputStream(fileOut);
        out.writeObject(data);
        out.close();
        fileOut.close();

        cleanUpFiles(p);

    }

    public void saveIniFile(SerializedPalletDetails data) throws IOException {

        File iniFile = getFile("palletimage", "ini");

        BufferedWriter writer = new BufferedWriter(new FileWriter(iniFile));
        for (String line: data.getImageParameters().getIniFileAsStrings()) {
            if (line != null) {
                writer.write(line + "\r\n");
            }
        }
        writer.close();

    }

    public void saveCameraPacket(SerializedPalletDetails data) throws IOException {

        String fileName = data.getPalletMessage().replace(":","_");
        File cameraPacket = getFile(fileName, "bin");

        FileOutputStream fileOut = new FileOutputStream(cameraPacket.toString());
        ObjectOutputStream out = new ObjectOutputStream(fileOut);
        out.writeObject(data.getCameraByteArray());
        out.close();
        fileOut.close();

    }

    public static <T> T readSerializedData(String fileName) throws IOException, ClassNotFoundException {

        FileInputStream fileIn = new FileInputStream(fileName);
        ObjectInputStream in = new ObjectInputStream(fileIn);
        T toReturn = (T) in.readObject();
        in.close();
        fileIn.close();
        return toReturn;

    }

    public static SerializedPalletDetails readCameraPacket(String path, String filename) throws IOException, ClassNotFoundException {

        String creationString = filename.replace("_",":");
        creationString = creationString.replace(".bin","");

        List<Integer> cameraPacket = readSerializedData(path + filename);

        SerializedPalletDetails spd = new SerializedPalletDetails(creationString);
        spd.setCameraByteArray(cameraPacket);

        return spd;

    }

}
