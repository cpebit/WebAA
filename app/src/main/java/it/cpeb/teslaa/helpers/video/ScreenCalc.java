package it.cpeb.teslaa.helpers.video;

import static java.lang.Integer.parseInt;

import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;

import it.cpeb.teslaa.proto.VideoCodecResolutionType;

public class ScreenCalc {

    public static int screenWidth;
    public static int screenHeight;
    private static float screenRatio;


    public static void SetScreenCalc(int screenWidth, int screenHeight) {
        screenRatio = (float) screenWidth / screenHeight;
        Log.d("CarScreen", "Screen ratio: " + screenRatio + "width: " + screenWidth + "height: " + screenHeight);
        ScreenCalc.screenWidth = screenWidth;
        ScreenCalc.screenHeight = screenHeight;
    }


    public static Integer getHeightMargin(int res) {

        if (getAAScreenHeightFor(res) > screenHeight && getAAScreenWidthFor(res) > screenWidth)
            return (getAAScreenHeightFor(res) - ScreenCalc.screenHeight);


        int margin = 0;
        float remoteRatio = (float) getAAScreenWidthFor(res) / getAAScreenHeightFor(res);
        if (screenRatio > remoteRatio)
            margin = getAAScreenHeightFor(res) - Math.round(getAAScreenWidthFor(res) / screenRatio);
        return margin;
    }

    public static Integer getWidthMargin(int res) {
        if (getAAScreenHeightFor(res) > screenHeight && getAAScreenWidthFor(res) > screenWidth)

            return (getAAScreenWidthFor(res) - ScreenCalc.screenWidth);


        int margin = 0;
        float remoteRatio = (float) getAAScreenWidthFor(res) / getAAScreenHeightFor(res);
        if (screenRatio <= remoteRatio)
            margin = Math.round(getAAScreenWidthFor(res) - (getAAScreenHeightFor(res) * screenRatio));

        return margin;
    }


    public static int getAAScreenWidthFor(String a) {
        return getAAScreenWidthFor(safeParseInt(a));
    }

    public static int getAAScreenHeightFor(String a) {
        return getAAScreenHeightFor(safeParseInt(a));
    }

    public static Integer getAAScreenWidthFor(int a) {
        switch (a) {
            case 0:
                return 800;
            case 1:
                return 1280;
            case 2:
                return 1920;
            case 3:
                return 2560;
            case 4:
                return 3840;
            case 5:
                return 720;
            case 6:
                return 1080;
            case 7:
                return 1440;
            default:
                return 2160;
        }
    }

    public static Integer getAAScreenHeightFor(int a) {
        switch (a) {
            case 0:
                return 480;
            case 1:
                return 720;
            case 2:
                return 1080;
            case 3:
                return 1440;
            case 4:
                return 2160;
            case 5:
                return 1280;
            case 6:
                return 1920;
            case 7:
                return 2560;
            default:
                return 3840;
        }
    }

    private static int safeParseInt(String a) {
        try {
            return parseInt(a);
        } catch (Exception e) {
            return 0;
        }
    }

    private int getNavigationBarHeight(Display display) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            DisplayMetrics metrics = new DisplayMetrics();
            display.getMetrics(metrics);
            int usableHeight = metrics.heightPixels;
            display.getRealMetrics(metrics);
            int realHeight = metrics.heightPixels;
            if (realHeight > usableHeight)
                return realHeight - usableHeight;
            else
                return 0;
        }
        return 0;
    }

    public static VideoCodecResolutionType getCodec(int a) {

        return VideoCodecResolutionType.forNumber(a + 1);

    }
}
