package it.cpeb.teslaa.helpers.messageprocessors;

import static it.cpeb.teslaa.TransporterService.mediaSocket;
import static it.cpeb.teslaa.TransporterService.notificationSocket;
import static it.cpeb.teslaa.helpers.Constants.AUDIO_MEDIA;
import static it.cpeb.teslaa.helpers.Constants.AUDIO_SYSTEM;
import static it.cpeb.teslaa.helpers.Constants.AUDIO_TTS;
import static it.cpeb.teslaa.helpers.Constants.FLAG_ALL;
import static it.cpeb.teslaa.helpers.Constants.VIDEOCHANNEL;
import static it.cpeb.teslaa.helpers.audio.AudioPlayer.audiosessions;

import org.greenrobot.eventbus.EventBus;

import java.util.Arrays;

import it.cpeb.teslaa.TransporterService;
import it.cpeb.teslaa.helpers.AAMessage;
import it.cpeb.teslaa.helpers.audio.AudioPlayer;
import it.cpeb.teslaa.helpers.video.JpegDecoder;
import it.cpeb.teslaa.proto.Ack;
import it.cpeb.teslaa.proto.Config;
import it.cpeb.teslaa.proto.MediaMessageId;
import it.cpeb.teslaa.proto.Start;


public class MediaMessage {

    private static final int MAX_UNACK = 1; //Upping to 10 gets FPS close to 25 on 1080p, good enough :)


    public static AAMessage mediaResponse(AAMessage received, int i) throws Exception {

        AAMessage resp;
        MediaMessageId msgtype = MediaMessageId.forNumber(received.getMessageID());


        if (msgtype == MediaMessageId.MEDIA_MESSAGE_STOP && received.getFlag() == FLAG_ALL && received.getChannel() != VIDEOCHANNEL) {
           // AudioPlayer.stop(received.getChannel());
            return null;
        }

        if (msgtype == MediaMessageId.MEDIA_MESSAGE_SETUP && received.getFlag() == FLAG_ALL) //Setup Request
        {

            Config.Builder avresp = Config.newBuilder();
            resp = new AAMessage(received.getChannel(), FLAG_ALL, MediaMessageId.MEDIA_MESSAGE_CONFIG_VALUE);
            avresp.setMaxUnacked(MAX_UNACK);

            avresp.setStatus(Config.Status.STATUS_READY);
            if (received.getChannel() == VIDEOCHANNEL) {
                TransporterService.videoReady = true;
                avresp.setMaxUnacked(1);
                avresp.addConfigurationIndices(i);
                //toogleVideoFocus(true);
            }
            resp.setData(avresp);
            return resp;
        }
        if (msgtype == MediaMessageId.MEDIA_MESSAGE_START && received.getFlag() == FLAG_ALL) //Start Request
        {
            received.trim();
            Start start = Start.parseFrom(received.getData());
            audiosessions.put(received.getChannel(), start.getSessionId());

       //     if (received.getChannel() != VIDEOCHANNEL)
         //       AudioPlayer.play(received.getChannel());


            return null;
        }
        if (received.getChannel() == AUDIO_MEDIA){

            if (msgtype == MediaMessageId.MEDIA_MESSAGE_DATA)
                mediaSocket.PushToBrowser(Arrays.copyOfRange(received.getData(),10,received.getData().length),false);
               //bas.write(received.getData(),10,received.getData().length-10);

                //AudioPlayer.write(received.getChannel(), received.getData(), 10);

            else
                //bas.write(received.getData(),2,received.getData().length-2);
                mediaSocket.PushToBrowser(Arrays.copyOfRange(received.getData(),2,received.getData().length),false);
                //AudioPlayer.write(received.getChannel(), received.getData(), 2);
            //  baos.write(received.getData(),2,received.getData().length-2);

            Ack.Builder ack = Ack.newBuilder();
            ack.setSessionId(audiosessions.get(AUDIO_MEDIA));
            ack.setAck(1);
            AAMessage ackrsp = new AAMessage(AUDIO_MEDIA, FLAG_ALL, MediaMessageId.MEDIA_MESSAGE_ACK_VALUE, ack);

           return ackrsp;




        }

        else if ( received.getChannel() == AUDIO_SYSTEM || received.getChannel() == AUDIO_TTS) {
            if (msgtype == MediaMessageId.MEDIA_MESSAGE_DATA)
                notificationSocket.PushToBrowser(Arrays.copyOfRange(received.getData(),10,received.getData().length),false);
            else
                notificationSocket.PushToBrowser(Arrays.copyOfRange(received.getData(),2,received.getData().length),false);

            Ack.Builder ack = Ack.newBuilder();
            ack.setSessionId(audiosessions.get(received.getChannel()));
            ack.setAck(1);
            AAMessage ackrsp = new AAMessage(received.getChannel(), FLAG_ALL, MediaMessageId.MEDIA_MESSAGE_ACK_VALUE, ack);
            return ackrsp;
        }
        else {


            JpegDecoder.buildNal(received.getFlag(), received.getData(), received.getPayload(), msgtype);
            return null;

        }


        //}
        //return null;
    }


}
