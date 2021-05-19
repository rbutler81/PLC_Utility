package com.cimcorp.plc.util;

import logger.Logger;

import static com.cimcorp.plc.util.KeyValuePair.*;

public class PalletLogging {

    public static String logSearchDetails(Pallet p) {
        String r = "Pallet Search Details ";
        r = r
            + kVPToString("TrackingNumber",p.getTrackingNumber())
            + kVPToString("OD", p.getOd())
            + kVPToString("ID", p.getId())
            + kVPToString("SectionWidth", p.getSw())
            + kVPToString("SearchFrom_mm", p.getHeightThresholdMin())
            + kVPToString("SearchTo_mm", p.getHeightThresholdMax())
            + kVPToString("SearchDeviation_mm", p.getSwDeviation(), 0)
            + kVPToString("SearchRadiusFrom_px", p.getFromRadiusPixels())
            + kVPToString("SearchRadiusTo_px", p.getToRadiusPixels());
        return r;
    }

    public static void logExpectedStacks(Pallet p, Logger l) {
        String r = "";
        for (Stack stack: p.getExpectedStacks()) {
           r = "Expected Stack "
                + kVPToString("ExpectedStackId", stack.getStackId())
                + kVPToString("TireCount", stack.getTireQty())
                + kVPToString("ExpectedHeight", stack.getExpectedHeight());
                l.logAndPrint(r);
        }
    }

    public static void logSuspectedStacks(Pallet p, Logger l) {
        String r = "";
        for (SuspectedStack stack: p.getSuspectedStacks()) {
            r = "Suspected Stack "
                    + kVPToString("SuspectedStackId", stack.getStackId())
                    + kVPToString("x_px", stack.getxPixel())
                    + kVPToString("y_px", stack.getyPixel())
                    + kVPToString("HoughSum", stack.getHoughValue())
                    + kVPToString("HoughRadius_px", stack.getPixelRadius())
                    + kVPToString("MeasuredHeight_mm", stack.getMeasuredHeight())
                    + kVPToString("SampleSuccess_percent", stack.getSampleSuccessRate(), 2)
                    + kVPToString("SampleMean_mm", stack.getMsd().getMean(), 2)
                    + kVPToString("SampleStdDeviation_mm", stack.getMsd().getStdDeviation(), 2);
            l.logAndPrint(r);
        }
    }

    public static void logMatchedStacks(Pallet p, Logger l) {
        String r = "";
        for (Stack stack: p.getExpectedStacks()) {
            if (stack.isStackMatched()) {
                r = "Matched Stack "
                        + kVPToString("ExpectedStackId", stack.getStackId())
                        + kVPToString("SuspectedStackId", stack.getFromSuspectedStack().getStackId());
                l.logAndPrint(r);
            }
        }
    }

    public static void logUnMatchedStacks(Pallet p, Logger l) {
        String r = "";
        for (SuspectedStack stack: p.getUnmatchedStacks()) {
            r = "Unmatched Stack "
                    + kVPToString("SuspectedStackId", stack.getStackId());
            l.logAndPrint(r);
        }
    }

    public static void logFinalStacks(Pallet p, Logger l) {
        String r = "";
        for (Stack stack: p.getExpectedStacks()) {
            if (stack.isStackMatched()) {
                r = "Final Stack Parameters "
                        + kVPToString("TrackingNumber", p.getTrackingNumber())
                        + kVPToString("ExpectedStackId", stack.getStackId())
                        + kVPToString("x_px", stack.getxPixel())
                        + kVPToString("y_px", stack.getyPixel())
                        + kVPToString("x_UnadjustedDistanceToCenterOfFrame_mm", stack.getxDistanceFromCenter_mm(), 2)
                        + kVPToString("y_UnadjustedDistanceToCenterOfFrame_mm", stack.getyDistanceFromCenter_mm(), 2);
                if (p.getIp().isErrorCorrectionSkewAdjustmentEnabled()) {
                    r = r
                            + kVPToString("x_AdjustedDistanceToCenterOfFrame_mm", stack.getxDistanceFromCenterAdjusted_mm(), 2)
                            + kVPToString("y_AdjustedDistanceToCenterOfFrame_mm", stack.getyDistanceFromCenterAdjusted_mm(), 2);
                }
                r = r
                        + kVPToString("x_StackDistanceFromPalletOrigin_mm", stack.getxDistanceFromPalletOrigin_mm())
                        + kVPToString("y_StackDistanceFromPalletOrigin_mm", stack.getyDistanceFromPalletOrigin_mm());
                l.logAndPrint(r);
            }
        }
    }

}
