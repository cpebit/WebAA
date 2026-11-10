package it.cpeb.teslaa.helpers.video;

import static it.cpeb.teslaa.TransporterService.surfaceSocket;
import static it.cpeb.teslaa.helpers.Constants.FLAG_ALL;
import static it.cpeb.teslaa.helpers.Constants.FLAG_FIRST;
import static it.cpeb.teslaa.helpers.Constants.FLAG_LAST;
import static it.cpeb.teslaa.helpers.Constants.FLAG_MIDDLE;
import static it.cpeb.teslaa.helpers.Constants.VIDEOCHANNEL;
import static it.cpeb.teslaa.helpers.audio.AudioPlayer.audiosessions;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.preference.PreferenceManager;

import org.greenrobot.eventbus.EventBus;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import it.cpeb.teslaa.TransporterService;
import it.cpeb.teslaa.helpers.AAMessage;
import it.cpeb.teslaa.proto.Ack;
import it.cpeb.teslaa.proto.MediaMessageId;
import it.cpeb.teslaa.proto.VideoFocusMode;
import it.cpeb.teslaa.proto.VideoFocusNotification;


public class JpegDecoder {



    private static final ByteArrayOutputStream nalUnit = new ByteArrayOutputStream();
    public static final ExecutorService ackThread = Executors.newSingleThreadExecutor();

    private final static String TAG = "JpegDecoder";

    public static void process(byte[] data) {
            surfaceSocket.PushToBrowser(data);
        }


    public static void buildNal(byte flag, byte[] decoded, int payload, MediaMessageId msgtype) throws IOException {
        if (flag == FLAG_ALL && msgtype == MediaMessageId.MEDIA_MESSAGE_DATA)   // If Not fragmented Video
        // Decode H264 video
        {
            process(Arrays.copyOfRange(decoded, 10, decoded.length));
        } else if (flag == FLAG_ALL && msgtype == MediaMessageId.MEDIA_MESSAGE_CODEC_CONFIG && (decoded[2] == 0 && decoded[3] == 0 && decoded[4] == 0 && decoded[5] == 1))                    // If Not fragmented First video config packet
        {
            process(Arrays.copyOfRange(decoded, 2, decoded.length));
        } else if (flag == FLAG_FIRST && msgtype == MediaMessageId.MEDIA_MESSAGE_DATA) {   // If First fragment Video
            nalUnit.reset();
            nalUnit.write(decoded, 10, decoded.length - 10);
        } else if (flag == FLAG_FIRST && msgtype == MediaMessageId.MEDIA_MESSAGE_CODEC_CONFIG) {   // If First fragment Video
            nalUnit.reset();
            nalUnit.write(decoded, 2, decoded.length - 2);
        } else if (flag == FLAG_MIDDLE) {
            nalUnit.write(decoded);
        } else if (flag == FLAG_LAST) {
            nalUnit.write(decoded);
            process(nalUnit.toByteArray());
        } else if (flag == 127) {

            process(decoded);
        }

    }


    private static Boolean hasfocus = null;

    public static void toogleVideoFocus(boolean focus) {

        if (!TransporterService.videoReady && focus) // video focus request too early!
        {
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    toogleVideoFocus(true);
                }
            }, 300);
            return;
        }


        if (Boolean.valueOf(focus).equals(hasfocus)) {
            System.out.println("Same focus request, igrnoring: "+focus);
            return;
        }


        hasfocus=focus;

        VideoFocusNotification.Builder videofocus = VideoFocusNotification.newBuilder();
        videofocus.setFocus(focus ? VideoFocusMode.VIDEO_FOCUS_PROJECTED : VideoFocusMode.VIDEO_FOCUS_NATIVE);
        videofocus.setUnsolicited(true);
        Log.d("HU-MEDIACODEC", "Sending video focus: " + videofocus);
        AAMessage aa = new AAMessage(VIDEOCHANNEL, FLAG_ALL, MediaMessageId.MEDIA_MESSAGE_VIDEO_FOCUS_NOTIFICATION_VALUE, videofocus);
        EventBus.getDefault().post(aa);


    }


}
