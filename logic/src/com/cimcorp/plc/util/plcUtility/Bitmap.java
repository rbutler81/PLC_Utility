package com.cimcorp.plc.util.plcUtility;

import java.awt.*;
import java.awt.image.BufferedImage;

public class Bitmap {

    String filename;
    int xResolution;
    int yResolution;
    String[] line;


    public Bitmap(String filename, int xResolution, int yResolution) {
        this.filename = filename;
        this.xResolution = xResolution;
        this.yResolution = yResolution;
        line = new String[yResolution];

        for (int y = 0; y < yResolution; y++) {
            line [y] = "";
            for (int x = 0; x < xResolution; x++) {
                line[y] = line[y] + "0";
            }
        }
    }

    public void addLine(int lineNumber, String s) {
        line[lineNumber] = s;
    }

    public BufferedImage complete() {
        BufferedImage img = new BufferedImage(xResolution, yResolution, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < yResolution; y++) {
            for (int x = 0; x < xResolution; x++) {
                String str = line[y].substring(x,x+1);
                if (str.equals("0")) {
                    img.setRGB(x, y, Color.WHITE.getRGB());
                }
                else if (str.equals("1")) {
                    img.setRGB(x, y, Color.BLACK.getRGB());
                }
            }
        }
        return img;
    }

    public String getFilename() {
        return filename;
    }
}
