package com.cimcorp.plc.util.palletImaging;

import java.io.Serializable;
import java.util.List;

public class SerializedPalletDetails implements Serializable {

    private static final long serialVersionUID = 2L;

    private String palletMessage;
    private ImageParameters imageParameters;
    private List<Integer> cameraByteArray;
    private int trackingNumber;

    public SerializedPalletDetails(String palletMessage) {
        this.palletMessage = palletMessage;
    }

    public String getPalletMessage() {
        return palletMessage;
    }

    public SerializedPalletDetails setPalletMessage(String palletMessage) {
        this.palletMessage = palletMessage;
        return this;
    }

    public ImageParameters getImageParameters() {
        return imageParameters;
    }

    public SerializedPalletDetails setImageParameters(ImageParameters imageParameters) {
        this.imageParameters = imageParameters;
        return this;
    }

    public List<Integer> getCameraByteArray() {
        return cameraByteArray;
    }

    public SerializedPalletDetails setCameraByteArray(List<Integer> cameraByteArray) {
        this.cameraByteArray = cameraByteArray;
        return this;
    }

    public int getTrackingNumber() {
        return trackingNumber;
    }

    public SerializedPalletDetails setTrackingNumber(int trackingNumber) {
        this.trackingNumber = trackingNumber;
        return this;
    }
}
