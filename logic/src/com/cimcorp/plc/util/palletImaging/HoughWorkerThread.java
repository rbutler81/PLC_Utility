package com.cimcorp.plc.util.palletImaging;

import com.cimcorp.communications.threads.Message;

public class HoughWorkerThread implements Runnable {

    Message<HoughMessage> msg = new Message<>();
    int radius;
    int[][] houghArray;
    boolean[][] edgeImage;
    int thetaIncrement;

    public HoughWorkerThread(int radius, int thetaIncrement, boolean[][] edgeImage, Message<HoughMessage> msg) {
        this.msg = msg;
        this.thetaIncrement = thetaIncrement;
        this.radius = radius;
        this.edgeImage = edgeImage;
    }

    public Message<HoughMessage> getMsg() {
        return msg;
    }

    public HoughWorkerThread setMsg(Message<HoughMessage> msg) {
        this.msg = msg;
        return this;
    }

    @Override
    public void run() {

        houghArray = ImageProcessing.oneRadiusHoughIteration(edgeImage, radius, thetaIncrement);
        msg.addMsg(new HoughMessage(radius, houghArray));

    }
}
