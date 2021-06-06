package com.cimcorp.plc.util.palletImaging;

import java.io.Serializable;

public class Square implements Serializable {

    private static final long serialVersionUID = 1L;

    private final int topLeftBoundary_x;
    private final int bottomRightBoundary_x;
    private final int topLeftBoundary_y;
    private final int bottomRightBoundary_y;

    int topLeft_x;
    int topLeft_y;
    int bottomLeft_x;
    int bottomRight_y;
    int imageResolution_x;
    int imageResolution_y;
    int pixels;

    public Square(int topLeft_x, int topLeft_y, int bottomRight_x, int bottomRight_y, int imageResolution_x, int imageResolution_y) throws ValueOutOfRangeException {
        this.topLeft_x = topLeft_x;
        this.topLeft_y = topLeft_y;
        this.bottomLeft_x = bottomRight_x;
        this.bottomRight_y = bottomRight_y;
        this.imageResolution_x = imageResolution_x;
        this.imageResolution_y = imageResolution_y;

        // check values to make sure they define a box
        if (valIsBetween(0,topLeft_x,imageResolution_x)) {
            if (valIsBetween(topLeft_x+1,bottomRight_x,imageResolution_x)) {
                // x values are ok
                if (valIsBetween(0,topLeft_y,imageResolution_y)) {
                    if (valIsBetween(topLeft_y+1,bottomRight_y,imageResolution_y)) {
                        // all values are ok
                        this.topLeftBoundary_x = topLeft_x;
                        this.bottomRightBoundary_x = bottomRight_x;
                        this.topLeftBoundary_y = topLeft_y;
                        this.bottomRightBoundary_y = bottomRight_y;
                    } else {
                        // second y value is not ok
                        throw new ValueOutOfRangeException("BottomRightCrop_y",bottomRight_y,topLeft_y+1,imageResolution_y);
                    }
                } else {
                    // first y value is not ok
                    throw new ValueOutOfRangeException("TopLeftCrop_y",topLeft_y,0,imageResolution_y);
                }
            } else {
                // second x value is not ok
                throw new ValueOutOfRangeException("BottomRightCrop_x",bottomRight_x,topLeft_x+1,imageResolution_x);
            }
        } else {
            // first x value is not ok
            throw new ValueOutOfRangeException("TopLeftCrop_x",topLeft_x,0,imageResolution_x);
        }

        pixels = (bottomRightBoundary_x - topLeftBoundary_x) * (bottomRightBoundary_y - topLeftBoundary_y);

    }

    public int getTopLeftBoundary_x() {
        return topLeftBoundary_x;
    }

    public int getBottomRightBoundary_x() {
        return bottomRightBoundary_x;
    }

    public int getTopLeftBoundary_y() {
        return topLeftBoundary_y;
    }

    public int getBottomRightBoundary_y() {
        return bottomRightBoundary_y;
    }

    private static boolean valIsBetween(int min, int val, int max) {
        if ((val >= min) && (val <= max)) {
            return true;
        } else {
            return false;
        }
    }

    public int getPixels() {
        return pixels;
    }
}
