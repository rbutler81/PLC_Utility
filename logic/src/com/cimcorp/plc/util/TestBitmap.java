package com.cimcorp.plc.util;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TestBitmap {

    static void test(int[][] data) {
        int height = data.length;
        int width = data[0].length;

        int[] flattenedData = new int[width*height*3];
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        int ind = 0;
        for (int i = 0; i < height; i++)
        {
            for (int j = 0; j < width; j++)
            {
                int greyShade = data[i][j];
                flattenedData[ind + j*3] = greyShade;
                flattenedData[ind + j*3+1] = greyShade;
                flattenedData[ind + j*3+2] = greyShade;

            }
            ind += height*3;
        }

        img.getRaster().setPixels(0, 0, 100, 100, flattenedData);

        // write to disk
        String path = Paths.get(".").toAbsolutePath().normalize().toString() + "\\";
        String filename = "test";
        Path p = Paths.get(path);
        if (!Files.exists(p)) {
            try {
                Files.createDirectories(p);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        File file = new File(path + filename + ".bmp");
        try {
            file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (file.exists()) {
            if (file.canWrite()) {

                try {
                    ImageIO.write(img, "BMP", file);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                System.out.println();
            }
        }
    }

}
