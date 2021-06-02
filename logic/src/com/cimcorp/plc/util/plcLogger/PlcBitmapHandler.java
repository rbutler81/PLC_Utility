package com.cimcorp.plc.util.plcLogger;

import com.cimcorp.plc.util.palletImaging.PalletDataFiles;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class PlcBitmapHandler {

    String path;
    int imagesToKeep;
    List<BitmapFromPlc> bitmapFromPlcs = new ArrayList<>();


    public PlcBitmapHandler(String path, int imagesToKeep) {
        this.path = path;
        this.imagesToKeep = imagesToKeep;
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
            for (BitmapFromPlc bitmapFromPlc : bitmapFromPlcs) {
                if (bitmapFromPlc.getFilename().equals(path + filename)) {
                    img = bitmapFromPlc.complete();
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

    public void writeImageToDisk(String filename, BufferedImage img) throws IOException {

        PalletDataFiles palletDataFiles = new PalletDataFiles(imagesToKeep, "");
        palletDataFiles.writeToDiskWithPath(filename, img, Paths.get(path));

    }
}
