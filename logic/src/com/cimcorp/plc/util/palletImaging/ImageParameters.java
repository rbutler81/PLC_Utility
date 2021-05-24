package com.cimcorp.plc.util.palletImaging;

import com.cimcorp.configFile.BD;
import com.cimcorp.configFile.Config;
import com.cimcorp.configFile.ParamRangeException;
import com.cimcorp.plc.util.Polynomial;

import java.math.BigDecimal;

public class ImageParameters {

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
    // algorithm parameters
    private int floorDistanceFromCamera;
    private int palletHeightFromFloor;
    private int toFromPixelSearchAdjust;
    private BigDecimal searchForRadiusDeviation;
    private int houghCirclePoints;
    private int tireSamplePoints;
    private boolean flipImageHorizontally;
    private int acceptedSampleSuccessPercent;
    private int distanceToCenterOfFrameFromPalletOriginMM_x;
    private int distanceToCenterOfFrameFromPalletOriginMM_y;
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

    // calculated parameters
    private int houghThetaIncrement;
    private int sampleThetaIncrement;

    public ImageParameters(Config config) throws ParamRangeException {

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

        this.floorDistanceFromCamera = config.getSingleParamAsInt("FloorDistanceFromCamera", 0);
        this.palletHeightFromFloor = config.getSingleParamAsInt("PalletHeightFromFloor", 0);
        this.searchForRadiusDeviation = config.getSingleParamAsBigDecimal("SearchForRadiusDeviation", 0);
        this.houghCirclePoints = config.getSingleParamAsInt("HoughCirclePoints", 4, 360);
        this.tireSamplePoints = config.getSingleParamAsInt("TireSamplePoints", 4, 360);
        this.toFromPixelSearchAdjust = config.getSingleParamAsInt("ToFromPixelSearchAdjust", 0, 10);
        this.flipImageHorizontally = config.getSingleParamAsBool("FlipImageHorizontally");
        this.acceptedSampleSuccessPercent = config.getSingleParamAsInt("AcceptedSampleSuccessPercent", 1, 100);
        this.distanceToCenterOfFrameFromPalletOriginMM_x = config.getSingleParamAsInt("DistanceToCenterOfFrameFromPalletOriginMM_x");
        this.distanceToCenterOfFrameFromPalletOriginMM_y = config.getSingleParamAsInt("DistanceToCenterOfFrameFromPalletOriginMM_y");

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

        this.houghThetaIncrement = BD.divide(360, houghCirclePoints);
        this.sampleThetaIncrement = BD.divide(360, tireSamplePoints);

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

    public int getToFromPixelSearchAdjust() {
        return toFromPixelSearchAdjust;
    }

    public boolean isFlipImageHorizontally() {
        return flipImageHorizontally;
    }

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
}

