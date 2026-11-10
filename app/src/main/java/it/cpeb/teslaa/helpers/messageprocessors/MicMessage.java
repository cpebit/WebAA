package it.cpeb.teslaa.helpers.messageprocessors;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static it.cpeb.teslaa.helpers.Constants.FLAG_ALL;
import static it.cpeb.teslaa.helpers.Constants.MICCHANNEL;

import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.util.Log;

import com.google.protobuf.InvalidProtocolBufferException;

import org.greenrobot.eventbus.EventBus;

import java.nio.ByteBuffer;

import it.cpeb.teslaa.activities.MicActivity;
import it.cpeb.teslaa.helpers.AAMessage;
import it.cpeb.teslaa.proto.MediaMessageId;
import it.cpeb.teslaa.proto.MicrophoneRequest;
import it.cpeb.teslaa.proto.MicrophoneResponse;


public class MicMessage {


    public static final int FREQUENCY = 16000;
    AAMessage resp = null;
    private final Context context;


    public MicMessage(Context context) {

        this.context = context;


    }


    private boolean micRunning;


    public AAMessage processMessage(AAMessage received) throws InvalidProtocolBufferException {

        if (received.getMessageID() != MediaMessageId.MEDIA_MESSAGE_MICROPHONE_REQUEST_VALUE)
            return null;


        received.trim();
        MicrophoneRequest openrequest = MicrophoneRequest.parseFrom(received.getData());
        Log.d("Mic", "Message: " + openrequest);
        if (openrequest.getOpen()) {

            //AudioPlayer.stopAll();
            micRunning = true;

            Intent intent = new Intent(context, MicActivity.class);
            intent.setFlags(FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            resp = new AAMessage(MICCHANNEL, FLAG_ALL, MediaMessageId.MEDIA_MESSAGE_MICROPHONE_RESPONSE_VALUE);
            MicrophoneResponse.Builder av = MicrophoneResponse.newBuilder();
            av.setSessionId(0);
            av.setStatus(0);
            resp.setData(av);
            return resp;
        } else {
            Intent intent = new Intent(context, MicActivity.class);
            intent.setFlags(FLAG_ACTIVITY_NEW_TASK);
            intent.setAction("it.cpeb.teslaa.STOP");
            context.startActivity(intent);

//            AudioPlayer.startAll();
            return null;
        }


    }


    private void sendMicData(byte[] ba) {
        try {
            ByteBuffer temp = ByteBuffer.allocate(ba.length + 10);
            temp.put(new byte[]{0, 0});
            temp.putLong(SystemClock.currentThreadTimeMillis());
            temp.put(ba);
            Log.d("Mic", "Send mic data to phone");
            AAMessage aamsg = new AAMessage(MICCHANNEL, FLAG_ALL, 0, temp.array());
            EventBus.getDefault().post(aamsg);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void stop() {
        Log.d("Mic", "Stop mic");
        //   RecorderSingleton.instance.stop();
    }
}
