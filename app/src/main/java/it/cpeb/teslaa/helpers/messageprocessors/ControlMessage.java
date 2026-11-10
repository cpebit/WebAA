package it.cpeb.teslaa.helpers.messageprocessors;

import static it.cpeb.teslaa.helpers.Constants.CONTROLCHANNEL;
import static it.cpeb.teslaa.helpers.Constants.FLAG_ALL;

import android.content.Context;

import java.io.IOException;

import it.cpeb.teslaa.BuildCarConfig;
import it.cpeb.teslaa.helpers.AAMessage;
import it.cpeb.teslaa.proto.AudioFocusNotification;
import it.cpeb.teslaa.proto.AudioFocusRequestNotification;
import it.cpeb.teslaa.proto.AudioFocusRequestType;
import it.cpeb.teslaa.proto.AudioFocusStateType;
import it.cpeb.teslaa.proto.ControlMessageType;
import it.cpeb.teslaa.proto.NavFocusNotification;
import it.cpeb.teslaa.proto.NavFocusType;
import it.cpeb.teslaa.proto.PingRequest;
import it.cpeb.teslaa.proto.PingResponse;


public class ControlMessage {

    public static AAMessage controlResponse(AAMessage received, Context context) throws IOException {
        AAMessage resp = null;
        ControlMessageType msgtype = ControlMessageType.forNumber(received.getMessageID());

        if (received.getFlag() == FLAG_ALL && msgtype == ControlMessageType.MESSAGE_SERVICE_DISCOVERY_REQUEST) {
            resp = BuildCarConfig.buildConf(context);
        } else if (received.getFlag() == FLAG_ALL && msgtype == ControlMessageType.MESSAGE_AUDIO_FOCUS_REQUEST) //Audio Focus Request, always 4 bytes
        {
            received.trim();
            resp = new AAMessage(CONTROLCHANNEL, FLAG_ALL, (short) 0, (short) 0, (short) ControlMessageType.MESSAGE_AUDIO_FOCUS_NOTIFICATION_VALUE, null);
            AudioFocusNotification.Builder audiofocusresp = AudioFocusNotification.newBuilder();
            AudioFocusRequestNotification audiofocusrequest = AudioFocusRequestNotification.parseFrom(received.getData());
            if (audiofocusrequest.getRequest() == AudioFocusRequestType.AUDIO_FOCUS_RELEASE)
                audiofocusresp.setFocusState(AudioFocusStateType.AUDIO_FOCUS_STATE_LOSS_TRANSIENT_CAN_DUCK);
            else
                audiofocusresp.setFocusState(AudioFocusStateType.AUDIO_FOCUS_STATE_GAIN);
            resp.setData(audiofocusresp);
        } else if (received.getFlag() == FLAG_ALL && msgtype == ControlMessageType.MESSAGE_NAV_FOCUS_REQUEST) {

            NavFocusNotification.Builder navresp = NavFocusNotification.newBuilder();
            navresp.setFocusType(NavFocusType.NAV_FOCUS_PROJECTED);
            resp = new AAMessage(CONTROLCHANNEL, FLAG_ALL, ControlMessageType.MESSAGE_NAV_FOCUS_NOTIFICATION_VALUE, navresp.build().toByteArray());

        } else if (received.getFlag() == FLAG_ALL && msgtype == ControlMessageType.MESSAGE_PING_REQUEST) {
            received.trim();
            resp = new AAMessage(CONTROLCHANNEL, FLAG_ALL, ControlMessageType.MESSAGE_PING_RESPONSE_VALUE, PingResponse.newBuilder().setTimestamp(PingRequest.parseFrom(received.getData()).getTimestamp()).build().toByteArray());
        } else if (received.getFlag() == FLAG_ALL && msgtype == ControlMessageType.MESSAGE_PING_RESPONSE) {
            resp = null;
        }

        return resp;
    }
}
