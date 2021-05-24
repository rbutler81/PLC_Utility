package com.cimcorp.plc.util.plcLogger;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

public class PlcBitmapHandler {

    String path;
    int bitmapsToKeep;
    List<BitmapFromPlc> bitmapFromPlcs = new ArrayList<>();

    public PlcBitmapHandler(String path, int bitmapsToKeep) {
        this.path = path;
        this.bitmapsToKeep = bitmapsToKeep;
    }

    public void parseLine(String s) throws IOException {

        if (s.contains("START")) {
            int lb = s.indexOf("[");
            int rb = s.indexOf("]");
            int comma = s.indexOf(",",lb);
            String xRes = s.substring(lb+1,comma);
            String yRes = s.substring(comma+1,rb);
            String filename = s.substring(rb+1);

            bitmapFromPlcs.add(new BitmapFromPlc(path + filename, Integer.parseInt(xRes), Integer.parseInt(yRes)));

        } else if (s.contains("STOP")) {

            int lb = s.indexOf("[");
            int rb = s.indexOf("]");
            String filename = s.substring(lb+1,rb);

            BufferedImage img = null;
            for (BitmapFromPlc b : bitmapFromPlcs) {
                if (b.getFilename().equals(path + filename)) {
                    img = b.complete();
                    break;
                }
            }

            writeImageToDisk(filename, img);

        }  else {
                int lb = s.indexOf("[");
                int rb = s.indexOf("]");
                int header = s.indexOf(",BMP,") + 5;
                String filename = s.substring(header,lb);
                String lineNumber = s.substring(lb+1,rb);
                String data = s.substring(rb+1);

                for (BitmapFromPlc b : bitmapFromPlcs) {
                    if (b.getFilename().equals(path + filename)) {
                        b.addLine(Integer.parseInt(lineNumber), data);
                    }
                }
        }

    }

    private void writeImageToDisk(String filename, BufferedImage img) throws IOException {

        // write to disk
        Path p = Paths.get(path);
        if (!Files.exists(p)) {
            Files.createDirectories(p);
        }

        File file = new File(path + filename + ".png");
        file.createNewFile();


        if (file.exists()) {
            if (file.canWrite()) {

                ImageIO.write(img, "png", file);

                for (int i = 0; i < bitmapFromPlcs.size(); i++) {
                    if (bitmapFromPlcs.get(i).getFilename().equals(path + filename)) {
                        bitmapFromPlcs.remove(i);
                        break;
                    }
                }
            }
        }

        // make a list of bmp files contained in the bitmap folder
        File[] files = new File(path).listFiles();
        List<File> bitmapList = new ArrayList<>();
        for (int i = 0; i < files.length; i++) {

            if (Pattern.compile(Pattern.quote(".bmp"), Pattern.CASE_INSENSITIVE).matcher(files[i].getName()).find()) {
                bitmapList.add(files[i]);
            }
        }

        // check if the number of bitmaps on the disk is greater than the configured amount - if so, delete the oldest ones first
        if (bitmapList.size() > bitmapsToKeep) {

            File[] bitmapFiles = new File[bitmapList.size()];
            bitmapList.toArray(bitmapFiles);
            Arrays.sort(bitmapFiles, Comparator.comparingLong(File::lastModified));
            int numberOfFilesToDelete = bitmapList.size() - bitmapsToKeep;
            for (int i = 0; i < numberOfFilesToDelete; i++) {
                bitmapFiles[i].delete();
            }
        }
    }
}
