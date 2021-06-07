package com.cimcorp.plc.util.palletImaging;

import com.cimcorp.logger.Logger;
import com.cimcorp.misc.helpers.TaskTimer;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static com.cimcorp.misc.helpers.KeyValuePair.kVPToString;

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
                if (p.getImageParameters().isErrorCorrectionSkewAdjustmentEnabled()) {
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

    public static void algoStats(Pallet p, int threads, TaskTimer timer, Logger logger) {
        String r = "";
        long algoTime = timer.checkTime();
        int radii = p.getToRadiusPixels() - p.getFromRadiusPixels();
        int edgePixels = p.getEdgePixels();

        r = r
            + kVPToString("ThreadsUsed",threads)
            + kVPToString("ProcessingTime_ms", algoTime)
            + kVPToString("RadiiSearched", radii)
            + kVPToString("EdgePixels", edgePixels);

        if (edgePixels > 0) {
            int xRes = p.getImageParameters().getCameraResolution_x();
            int yRes = p.getImageParameters().getCameraResolution_y();
            int pixelsInImage = xRes * yRes;
            BigDecimal edgePixelsAsPercentBd = new BigDecimal(edgePixels).setScale(10,RoundingMode.HALF_UP)
                    .divide(new BigDecimal(pixelsInImage),10,RoundingMode.HALF_UP)
                    .multiply(new BigDecimal(100));
            String edgePixelsAsPercent = edgePixelsAsPercentBd.setScale(2,RoundingMode.HALF_UP).toString();
            r = r + kVPToString("EdgePixelsPercentage",edgePixelsAsPercent);

            BigDecimal timePerPixelBd = new BigDecimal(algoTime).setScale(10, RoundingMode.HALF_UP)
                                .divide(new BigDecimal(edgePixels),10,RoundingMode.HALF_UP);
            String timePerPixel = timePerPixelBd.setScale(2, RoundingMode.HALF_UP).toString();
            r = r + kVPToString("TimePerPixel_ms",timePerPixel);
        }

        if (radii > 0) {
            BigDecimal timePerRadiusBd = new BigDecimal(algoTime).setScale(10,RoundingMode.HALF_UP)
                                .divide(new BigDecimal(radii),10,RoundingMode.HALF_UP);
            String timePerRadius = timePerRadiusBd.setScale(2,RoundingMode.HALF_UP).toString();
            r = r + kVPToString("TimePerRadius_ms",timePerRadius);
        }

        if ((radii > 0) && (edgePixels > 0)) {
            BigDecimal timePerPixelPerRadiusBd = new BigDecimal(algoTime).setScale(10, RoundingMode.HALF_UP)
                                .divide(new BigDecimal(edgePixels),10,RoundingMode.HALF_UP)
                                .divide(new BigDecimal(radii),10,RoundingMode.HALF_UP);
            String timePerPixelPerRadius = timePerPixelPerRadiusBd.setScale(4,RoundingMode.HALF_UP).toString();
            r = r + kVPToString("TimePerPixelPerRadius_ms",timePerPixelPerRadius);
        }

        logger.logAndPrint(r);

    }

    public static void postsDetected(Pallet p, Logger logger) {

        for (Post post: p.getDetectedPosts()) {
            String r = "Post Detected "
                    + post.toString();

            logger.logAndPrint(r);
        }

    }

}
