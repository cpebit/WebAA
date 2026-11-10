package it.cpeb.teslaa.helpers;

import static it.cpeb.teslaa.helpers.Constants.AUDIO_MEDIA;
import static it.cpeb.teslaa.helpers.Constants.AUDIO_SYSTEM;
import static it.cpeb.teslaa.helpers.Constants.AUDIO_TTS;
import static it.cpeb.teslaa.helpers.Constants.BLUETOOTHCHANNEL;
import static it.cpeb.teslaa.helpers.Constants.CONTROLCHANNEL;
import static it.cpeb.teslaa.helpers.Constants.FLAG_ALL;
import static it.cpeb.teslaa.helpers.Constants.FLAG_CONTROL;
import static it.cpeb.teslaa.helpers.Constants.INPUTCHANNEL;
import static it.cpeb.teslaa.helpers.Constants.MEDIAINFO;
import static it.cpeb.teslaa.helpers.Constants.MICCHANNEL;
import static it.cpeb.teslaa.helpers.Constants.NAVINFO;
import static it.cpeb.teslaa.helpers.Constants.SENORCHANNEL;
import static it.cpeb.teslaa.helpers.Constants.VIDEOCHANNEL;
import static it.cpeb.teslaa.proto.ControlMessageType.MESSAGE_CHANNEL_OPEN_REQUEST_VALUE;
import static it.cpeb.teslaa.proto.ControlMessageType.MESSAGE_CHANNEL_OPEN_RESPONSE_VALUE;

import android.content.Context;

import org.greenrobot.eventbus.EventBus;

import it.cpeb.teslaa.helpers.messageprocessors.ControlMessage;
import it.cpeb.teslaa.helpers.messageprocessors.InputMessage;
import it.cpeb.teslaa.helpers.messageprocessors.MediaInfoMessage;
import it.cpeb.teslaa.helpers.messageprocessors.MediaMessage;
import it.cpeb.teslaa.helpers.messageprocessors.MicMessage;
import it.cpeb.teslaa.helpers.messageprocessors.SensorMessage;
import it.cpeb.teslaa.proto.ChannelOpenResponse;
import it.cpeb.teslaa.proto.MessageStatus;


public class MessageProcessor {
    private static final String TAG = "MessgaeProcessor";

    private final MicMessage micMessage;
    private final int videoconfig;
    private final Context context;


    public MessageProcessor(Context context) {
        this.context = context;
        micMessage = new MicMessage(context);
        //videoconfig = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(context).getString("hires", "0"));
        videoconfig = 0;

    }

    public void process(AAMessage message) throws Exception {
        AAMessage response = null;

        // if ((message.getChannel() == MICCHANNEL || message.getChannel() == CONTROLCHANNEL))
        // Log.d(TAG, "Channel: " + message.getChannel() + " flag: " + message.getFlag() + "Message type: " + message.getMessageID() + "enclen: " + message.getEnc_len() + ", payload: " + message.getPayload() + " size: " + message.getData().length + ", data: " + Util.bytesToHex(message.getData()));

        if (message.getEnc_len() == 0 || message.getData().length == 0)
            return;
        if (message.getMessageID() == MESSAGE_CHANNEL_OPEN_REQUEST_VALUE && message.getType() == FLAG_CONTROL) {
            Log.d(TAG, "Open Channel: " + message.getChannel() + " flag: " + FLAG_CONTROL);
            response = new AAMessage(message.getChannel(), FLAG_ALL, MESSAGE_CHANNEL_OPEN_RESPONSE_VALUE, ChannelOpenResponse.newBuilder().setStatus(MessageStatus.STATUS_SUCCESS));
            response.setType(FLAG_CONTROL);


        } else {
            switch (message.getChannel()) {
                case CONTROLCHANNEL:
                    response = ControlMessage.controlResponse(message, context);
                    break;
                case SENORCHANNEL:
                    response = SensorMessage.sensorResponse(message);
                    break;
                case MICCHANNEL:
                    response = micMessage.processMessage(message);
                    break;
                case VIDEOCHANNEL:
                case AUDIO_MEDIA:
                case AUDIO_SYSTEM:
                case AUDIO_TTS:
                    response = MediaMessage.mediaResponse(message, videoconfig);
                    break;
                case INPUTCHANNEL:
                    response = InputMessage.inputResponse(message);
                    break;
                case MEDIAINFO:
                    MediaInfoMessage.MediaInfoMessage(message, context);
                    break;
                case NAVINFO:
                    Log.d(TAG, "Channel: " + message.getChannel() + " flag: " + message.getFlag() + "Message type: " + message.getMessageID() + "enclen: " + message.getEnc_len() + ", payload: " + message.getPayload() + " size: " + message.getData().length + ", data: " + Util.bytesToHex(message.getData()));


                    break;
                case BLUETOOTHCHANNEL:
                    //btMessage.processMessage(flag, msgtype, decoded);
                    break;
                default:
                    break;
            }
        }

        if (response != null)
            EventBus.getDefault().post(response);
        //inOutStream.write(response.getFormated(ssl));

    }
}
