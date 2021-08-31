package com.cimcorp.plc.util.palletImaging;

import com.cimcorp.configFile.Config;
import com.cimcorp.configFile.ParamRangeException;
import com.cimcorp.misc.math.BigDecimalMath;
import com.cimcorp.misc.math.Polynomial;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ImageParameters implements Serializable {

    private static final long serialVersionUID = 3L;

    // ini file as a list of strings
    private List<String> iniFileAsStrings = new ArrayList<>();

    // communication parameters
    private int listenerPort;
    private int remotePort;
    private String remoteIp;
    private int resendDelay;
    private int resendAttempts;
    // camera parameters
    private int cameraResolution_x;
    private int cameraResolution_y;
    private BigDecimal cameraFactor_x;
    private BigDecimal cameraFactor_y;
    private int cameraPacketSizeBytes;
    private int imageDataOffsetBytes;
    private String cameraIp;
    private int cameraPort;
    private int cameraTimeout;
    private int cameraRetries;
    // algorithm parameters
    private int floorDistanceFromCamera;
    private int palletHeightFromFloor;
    private int toPixelSearchAdjust;
    private int fromPixelSearchAdjust;
    private int minAcceptableDistanceFromCamera;
    private int maxAcceptableDistanceBelowFloor;
    private BigDecimal searchForRadiusDeviation;
    private int houghCirclePoints;
    private int tireSamplePoints;
    private boolean flipImageHorizontally;
    private boolean flipImageVertically;
    private int acceptedSampleSuccessPercent;
    private int distanceToCenterOfFrameFromPalletOriginMM_x;
    private int distanceToCenterOfFrameFromPalletOriginMM_y;
    private int threadsToUse;
    private boolean cropImageEnable;
    private int cropTopLeft_x;
    private int cropTopLeft_y;
    private int cropBottomRight_x;
    private int cropBottomRight_y;
    // debug parameters
    private boolean debugEnabled;
    private String debugFilename;
    private boolean useDebugFileParams;
    private boolean extractIniAsFile;
    private boolean extractImageAsFile;
    private boolean loadImageFile;
    private String imageFile;
    // error correction parameters
    private boolean errorCorrectionAverageMissingDataEnabled;
    private int errorCorrectionBlockSize;
    private boolean errorCorrectionHeightAdjustmentEnabled;
    private BigDecimal errorCorrectionHeightAdjustment_b;
    private BigDecimal errorCorrectionHeightAdjustment_m;

    private boolean errorCorrectionSkewAdjustmentEnabled;
    private Polynomial errorCorrectionQuadrant1xCoefficients;
    private Polynomial errorCorrectionQuadrant1yCoefficients;
    private Polynomial errorCorrectionQuadrant2xCoefficients;
    private Polynomial errorCorrectionQuadrant2yCoefficients;
    private Polynomial errorCorrectionQuadrant3xCoefficients;
    private Polynomial errorCorrectionQuadrant3yCoefficients;
    private Polynomial errorCorrectionQuadrant4xCoefficients;
    private Polynomial errorCorrectionQuadrant4yCoefficients;

    private boolean postDetectionEnabled;
    private int postHeight;
    private int postHeightDeviation;
    private int postSampleSuccessRate;
    private List<Square> postAreas = new ArrayList<>();

    // calculated parameters
    private int houghThetaIncrement;
    private int sampleThetaIncrement;

    public ImageParameters(Config config) throws ParamRangeException, ValueOutOfRangeException {

        this.iniFileAsStrings = config.getIniFileAsStrings();

        this.listenerPort = config.getSingleParamAsInt("ListenerPort", 1, 65535);
        this.remotePort = config.getSingleParamAsInt("RemotePort", 1, 65535);
        this.remoteIp = config.getSingleParamAsString("RemoteIp");
        this.resendDelay = config.getSingleParamAsInt("ResendDelay", 50, 5000);
        this.resendAttempts = config.getSingleParamAsInt("ResendAttempts", 0, 10);

        this.cameraResolution_x = config.getSingleParamAsInt("CameraResolution_x", 0);
        this.cameraResolution_y = config.getSingleParamAsInt("CameraResolution_y", 0);
        this.cameraFactor_x = config.getSingleParamAsBigDecimal("CameraFactor_x", 0);
        this.cameraFactor_y = config.getSingleParamAsBigDecimal("CameraFactor_y", 0);
        this.cameraPacketSizeBytes = config.getSingleParamAsInt("CameraPacketSizeBytes", 0);
        this.imageDataOffsetBytes = config.getSingleParamAsInt("ImageDataOffsetBytes", 0);
        this.cameraIp = config.getSingleParamAsString("CameraIp");
        this.cameraPort = config.getSingleParamAsInt("CameraPort", 1, 65535);
        this.cameraTimeout = config.getSingleParamAsInt("CameraTimeout", 1000, 30000);
        this.cameraRetries = config.getSingleParamAsInt("CameraRetries", 0, 10);

        this.floorDistanceFromCamera = config.getSingleParamAsInt("FloorDistanceFromCamera", 0);
        this.palletHeightFromFloor = config.getSingleParamAsInt("PalletHeightFromFloor", 0);
        this.searchForRadiusDeviation = config.getSingleParamAsBigDecimal("SearchForRadiusDeviation", 0);
        this.houghCirclePoints = config.getSingleParamAsInt("HoughCirclePoints", 4, 360);
        this.tireSamplePoints = config.getSingleParamAsInt("TireSamplePoints", 4, 360);
        this.toPixelSearchAdjust = config.getSingleParamAsInt("ToPixelSearchAdjust", -10, 10);
        this.fromPixelSearchAdjust = config.getSingleParamAsInt("FromPixelSearchAdjust", -10, 10);
        this.minAcceptableDistanceFromCamera = config.getSingleParamAsInt("MinAcceptableDistanceFromCamera", 0, 5000);
        this.maxAcceptableDistanceBelowFloor = config.getSingleParamAsInt("MaxAcceptableDistanceBelowFloor", 0, 5000);

        this.debugEnabled = config.getSingleParamAsBool("DebugModeEnabled");
        this.debugFilename = config.getSingleParamAsString("DebugFile");
        this.useDebugFileParams = config.getSingleParamAsBool("UseDebugFileParams");
        this.extractIniAsFile = config.getSingleParamAsBool("ExtractIniAsFile");
        this.extractImageAsFile = config.getSingleParamAsBool("ExtractImageAsFile");
        this.imageFile = config.getSingleParamAsString("ImageFile");
        this.loadImageFile = config.getSingleParamAsBool("LoadImageFile");

        this.flipImageHorizontally = config.getSingleParamAsBool("FlipImageHorizontally");
        this.flipImageVertically = config.getSingleParamAsBool("FlipImageVertically");
        this.acceptedSampleSuccessPercent = config.getSingleParamAsInt("AcceptedSampleSuccessPercent", 1, 100);
        this.distanceToCenterOfFrameFromPalletOriginMM_x = config.getSingleParamAsInt("DistanceToCenterOfFrameFromPalletOriginMM_x");
        this.distanceToCenterOfFrameFromPalletOriginMM_y = config.getSingleParamAsInt("DistanceToCenterOfFrameFromPalletOriginMM_y");

        this.cropImageEnable = config.getSingleParamAsBool("CropImageEnable");
        if (this.cropImageEnable) {

            List<String> topLeftPixels = config.getParam("TopLeftPixel");
            List<String> bottomRightPixels = config.getParam("BottomRightPixel");
            Square croppedArea = new Square(Integer.parseInt(topLeftPixels.get(0)),
                    Integer.parseInt(topLeftPixels.get(1)),
                    Integer.parseInt(bottomRightPixels.get(0)),
                    Integer.parseInt(bottomRightPixels.get(1)),
                    this.cameraResolution_x,
                    this.cameraResolution_y);
            this.cropTopLeft_x = croppedArea.topLeft_x;
            this.cropBottomRight_x = croppedArea.getBottomRightBoundary_x();
            this.cropTopLeft_y = croppedArea.topLeft_y;
            this.cropBottomRight_y = croppedArea.bottomRight_y;

         } else {
            // set cropping region to the entire image
            this.cropTopLeft_x = 0;
            this.cropBottomRight_x = cameraResolution_x;
            this.cropTopLeft_y = 0;
            this.cropBottomRight_y = cameraResolution_y;
        }

        this.postDetectionEnabled = config.getSingleParamAsBool("PostDetectionEnabled");
        if (this.postDetectionEnabled) {

            this.postHeight = config.getSingleParamAsInt("PostHeight");
            this.postHeightDeviation = config.getSingleParamAsInt("PostHeightDeviation");
            this.postSampleSuccessRate = config.getSingleParamAsInt("PostSampleSuccessRate");

            checkAndSetPostAreas(config);

        }

        this.threadsToUse = config.getSingleParamAsInt("ThreadsToUse", 1, 99);

        this.errorCorrectionAverageMissingDataEnabled = config.getSingleParamAsBool("ErrorCorrectionAverageMissingDataEnabled");
        this.errorCorrectionBlockSize = config.getSingleParamAsInt("ErrorCorrectionBlockSize", 3, 10);
        this.errorCorrectionHeightAdjustmentEnabled = config.getSingleParamAsBool("ErrorCorrectionHeightAdjustmentEnabled");
        this.errorCorrectionHeightAdjustment_b = config.getSingleParamAsBigDecimal("ErrorCorrectionHeightAdjustment_b");
        this.errorCorrectionHeightAdjustment_m = config.getSingleParamAsBigDecimal("ErrorCorrectionHeightAdjustment_m");

        this.errorCorrectionSkewAdjustmentEnabled = config.getSingleParamAsBool("ErrorCorrectionSkewAdjustmentEnabled");
        String [] coefficients = config.getParam("ErrorCorrectionQuadrant1xCoefficients").toArray(new String[0]);
        this.errorCorrectionQuadrant1xCoefficients = new Polynomial(coefficients,15);
        coefficients = config.getParam("ErrorCorrectionQuadrant1yCoefficients").toArray(new String[0]);
        this.errorCorrectionQuadrant1yCoefficients = new Polynomial(coefficients,15);
        coefficients = config.getParam("ErrorCorrectionQuadrant2xCoefficients").toArray(new String[0]);
        this.errorCorrectionQuadrant2xCoefficients = new Polynomial(coefficients,15);
        coefficients = config.getParam("ErrorCorrectionQuadrant2yCoefficients").toArray(new String[0]);
        this.errorCorrectionQuadrant2yCoefficients = new Polynomial(coefficients,15);
        coefficients = config.getParam("ErrorCorrectionQuadrant3xCoefficients").toArray(new String[0]);
        this.errorCorrectionQuadrant3xCoefficients = new Polynomial(coefficients,15);
        coefficients = config.getParam("ErrorCorrectionQuadrant3yCoefficients").toArray(new String[0]);
        this.errorCorrectionQuadrant3yCoefficients = new Polynomial(coefficients,15);
        coefficients = config.getParam("ErrorCorrectionQuadrant4xCoefficients").toArray(new String[0]);
        this.errorCorrectionQuadrant4xCoefficients = new Polynomial(coefficients,15);
        coefficients = config.getParam("ErrorCorrectionQuadrant4yCoefficients").toArray(new String[0]);
        this.errorCorrectionQuadrant4yCoefficients = new Polynomial(coefficients,15);

        this.houghThetaIncrement = BigDecimalMath.divide(360, houghCirclePoints);
        this.sampleThetaIncrement = BigDecimalMath.divide(360, tireSamplePoints);


    }

    private void checkAndSetPostAreas(Config config) throws ValueOutOfRangeException {

        for (int i = 1; i <= 4; i++) {

            String topLeft = "Post" + i + "_TopLeftPixel";
            String bottomRight = "Post" + i + "_BottomRightPixel";

            Square postArea = new Square(
                    Integer.parseInt(config.getParam(topLeft).get(0)),
                    Integer.parseInt(config.getParam(topLeft).get(1)),
                    Integer.parseInt(config.getParam(bottomRight).get(0)),
                    Integer.parseInt(config.getParam(bottomRight).get(1)),
                    this.cameraResolution_x,
                    this.cameraResolution_y);

            postAreas.add(postArea);
        }

    }

    private static boolean valIsBetween(int min, int val, int max) {
        if ((val >= min) && (val <= max)) {
            return true;
        } else {
            return false;
        }
    }

    public int getListenerPort() {
        return listenerPort;
    }

    public int getCameraResolution_x() {
        return cameraResolution_x;
    }

    public int getCameraResolution_y() {
        return cameraResolution_y;
    }

    public BigDecimal getCameraFactor_x() {
        return cameraFactor_x;
    }

    public BigDecimal getCameraFactor_y() {
        return cameraFactor_y;
    }

    public int getCameraPacketSizeBytes() {
        return cameraPacketSizeBytes;
    }

    public int getImageDataOffsetBytes() {
        return imageDataOffsetBytes;
    }

    public int getFloorDistanceFromCamera() {
        return floorDistanceFromCamera;
    }

    public int getPalletHeightFromFloor() {
        return palletHeightFromFloor;
    }

    public BigDecimal getSearchForRadiusDeviation() {
        return searchForRadiusDeviation;
    }

    public int getHoughCirclePoints() {
        return houghCirclePoints;
    }

    public int getTireSamplePoints() {
        return tireSamplePoints;
    }

    public boolean isErrorCorrectionAverageMissingDataEnabled() {
        return errorCorrectionAverageMissingDataEnabled;
    }

    public int getErrorCorrectionBlockSize() {
        return errorCorrectionBlockSize;
    }

    public int getHoughThetaIncrement() {
        return houghThetaIncrement;
    }

    public int getSampleThetaIncrement() {
        return sampleThetaIncrement;
    }

    public int getToPixelSearchAdjust() {
        return toPixelSearchAdjust;
    }

    public boolean isFlipImageHorizontally() { return flipImageHorizontally; }

    public boolean isFlipImageVertically() { return flipImageVertically; }

    public boolean isErrorCorrectionHeightAdjustmentEnabled() {
        return errorCorrectionHeightAdjustmentEnabled;
    }

    public BigDecimal getErrorCorrectionHeightAdjustment_b() {
        return errorCorrectionHeightAdjustment_b;
    }

    public BigDecimal getErrorCorrectionHeightAdjustment_m() {
        return errorCorrectionHeightAdjustment_m;
    }

    public int getAcceptedSampleSuccessPercent() {
        return acceptedSampleSuccessPercent;
    }

    public boolean isErrorCorrectionSkewAdjustmentEnabled() {
        return errorCorrectionSkewAdjustmentEnabled;
    }

    public Polynomial getErrorCorrectionQuadrant1xCoefficients() {
        return errorCorrectionQuadrant1xCoefficients;
    }

    public Polynomial getErrorCorrectionQuadrant1yCoefficients() {
        return errorCorrectionQuadrant1yCoefficients;
    }

    public Polynomial getErrorCorrectionQuadrant2xCoefficients() {
        return errorCorrectionQuadrant2xCoefficients;
    }

    public Polynomial getErrorCorrectionQuadrant2yCoefficients() {
        return errorCorrectionQuadrant2yCoefficients;
    }

    public Polynomial getErrorCorrectionQuadrant3xCoefficients() {
        return errorCorrectionQuadrant3xCoefficients;
    }

    public Polynomial getErrorCorrectionQuadrant3yCoefficients() {
        return errorCorrectionQuadrant3yCoefficients;
    }

    public Polynomial getErrorCorrectionQuadrant4xCoefficients() {
        return errorCorrectionQuadrant4xCoefficients;
    }

    public Polynomial getErrorCorrectionQuadrant4yCoefficients() {
        return errorCorrectionQuadrant4yCoefficients;
    }

    public int getDistanceToCenterOfFrameFromPalletOriginMM_x() {
        return distanceToCenterOfFrameFromPalletOriginMM_x;
    }

    public int getDistanceToCenterOfFrameFromPalletOriginMM_y() {
        return distanceToCenterOfFrameFromPalletOriginMM_y;
    }

    public int getRemotePort() {
        return remotePort;
    }

    public String getRemoteIp() {
        return remoteIp;
    }

    public int getResendDelay() {
        return resendDelay;
    }

    public int getResendAttempts() {
        return resendAttempts;
    }

    public String getCameraIp() {
        return cameraIp;
    }

    public int getCameraPort() {
        return cameraPort;
    }

    public int getCameraTimeout() {
        return cameraTimeout;
    }

    public int getCameraRetries() {
        return cameraRetries;
    }

    public int getThreadsToUse() {
        return threadsToUse;
    }

    public int getFromPixelSearchAdjust() {
        return fromPixelSearchAdjust;
    }

    public int getMinAcceptableDistanceFromCamera() {
        return minAcceptableDistanceFromCamera;
    }

    public int getCropTopLeft_x() {
        return cropTopLeft_x;
    }

    public int getCropTopLeft_y() {
        return cropTopLeft_y;
    }

    public int getCropBottomRight_x() {
        return cropBottomRight_x;
    }

    public int getCropBottomRight_y() {
        return cropBottomRight_y;
    }

    public boolean isDebugEnabled() {
        return debugEnabled;
    }

    public String getDebugFilename() {
        return debugFilename;
    }

    public boolean useDebugFileParams() {
        return useDebugFileParams;
    }

    public boolean isExtractIniAsFile() {
        return extractIniAsFile;
    }

    public int getMaxAcceptableDistanceBelowFloor() {
        return maxAcceptableDistanceBelowFloor;
    }

    public List<String> getIniFileAsStrings() {
        return iniFileAsStrings;
    }

    public ImageParameters setIniFileAsStrings(List<String> iniFileAsStrings) {
        this.iniFileAsStrings = iniFileAsStrings;
        return this;
    }

    public boolean isExtractImageAsFile() {
        return extractImageAsFile;
    }

    public boolean isLoadImageFile() {
        return loadImageFile;
    }

    public String getImageFile() {
        return imageFile;
    }

    public int getPostHeight() {
        return postHeight;
    }

    public int getPostHeightDeviation() {
        return postHeightDeviation;
    }

    public int getPostSampleSuccessRate() {
        return postSampleSuccessRate;
    }

    public List<Square> getPostAreas() {
        return postAreas;
    }

    public boolean isPostDetectionEnabled() {
        return postDetectionEnabled;
    }
}

