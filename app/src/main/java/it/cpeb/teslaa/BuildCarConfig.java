package it.cpeb.teslaa;

import static it.cpeb.teslaa.helpers.Constants.AUDIO_MEDIA;
import static it.cpeb.teslaa.helpers.Constants.AUDIO_SYSTEM;
import static it.cpeb.teslaa.helpers.Constants.AUDIO_TTS;
import static it.cpeb.teslaa.helpers.Constants.CONTROLCHANNEL;
import static it.cpeb.teslaa.helpers.Constants.FLAG_ALL;
import static it.cpeb.teslaa.helpers.Constants.INPUTCHANNEL;
import static it.cpeb.teslaa.helpers.Constants.MEDIAINFO;
import static it.cpeb.teslaa.helpers.Constants.MICCHANNEL;
import static it.cpeb.teslaa.helpers.Constants.SENORCHANNEL;
import static it.cpeb.teslaa.helpers.Constants.VIDEOCHANNEL;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import java.util.Arrays;

import it.cpeb.teslaa.helpers.AAMessage;
import it.cpeb.teslaa.helpers.Log;
import it.cpeb.teslaa.helpers.video.ScreenCalc;
import it.cpeb.teslaa.proto.AudioConfiguration;
import it.cpeb.teslaa.proto.AudioStreamType;
import it.cpeb.teslaa.proto.Common;
import it.cpeb.teslaa.proto.ControlMessageType;
import it.cpeb.teslaa.proto.DisplayType;
import it.cpeb.teslaa.proto.DriverPosition;
import it.cpeb.teslaa.proto.InputSourceService;
import it.cpeb.teslaa.proto.MediaCodecType;
import it.cpeb.teslaa.proto.MediaPlaybackStatusService;
import it.cpeb.teslaa.proto.MediaSinkService;
import it.cpeb.teslaa.proto.MediaSourceService;
import it.cpeb.teslaa.proto.SensorSourceService;
import it.cpeb.teslaa.proto.SensorType;
import it.cpeb.teslaa.proto.Service;
import it.cpeb.teslaa.proto.ServiceDiscoveryResponse;
import it.cpeb.teslaa.proto.TouchScreenType;
import it.cpeb.teslaa.proto.VideoConfiguration;
import it.cpeb.teslaa.proto.VideoFrameRateType;


public class BuildCarConfig {


    public static AAMessage buildConf(Context context) {


        SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(context);


        int dpi = SP.getInt("dpi", 140);
        // int dpi = 135;


        int hires = Integer.parseInt(SP.getString("resolution", "1"));
        boolean useBT = SP.getBoolean("useBT", true);


        ServiceDiscoveryResponse.Builder newdata = ServiceDiscoveryResponse.newBuilder();
        newdata.setHeadunitInfo(Common.HeadUnitInfo.newBuilder()
                .setHeadUnitMake("Google")
                .setHeadUnitModel("Desktop Head Unit")
                .setMake("Google")
                .setModel("Desktop Head Unit")
                .setHeadUnitSoftwareBuild("HUR")
                .setHeadUnitSoftwareVersion("1.1")

                .build());
        newdata.setDriverPosition(SP.getBoolean("rhd", false) ? DriverPosition.DRIVER_POSITION_RIGHT : DriverPosition.DRIVER_POSITION_LEFT);


        //Input Channel
        Service.Builder channelDescriptior = Service.newBuilder();
        InputSourceService.Builder inputchannel = InputSourceService.newBuilder();

        Integer[] supportedkeys;

        inputchannel.addTouchscreen(InputSourceService.TouchScreen.newBuilder().setType(TouchScreenType.CAPACITIVE).setHeight(ScreenCalc.getAAScreenHeightFor(hires)).setWidth(ScreenCalc.getAAScreenWidthFor(hires)).build());
         supportedkeys = new Integer[]{1, 2, 3, 4, 5, 6, 0x13, 0x14, 0x15, 0x16, 0x17, 0x54, 0x55, 0x57, 0x58};


        Iterable<Integer> iterable = Arrays.asList(supportedkeys);
        inputchannel.addAllKeycodesSupported(iterable);

        channelDescriptior.setInputSourceService(inputchannel.build());
        channelDescriptior.setId(INPUTCHANNEL);

        newdata.addServices(channelDescriptior.build());
        //Sensor Channel
        channelDescriptior.clear();
        SensorSourceService.Builder sensorchannel = SensorSourceService.newBuilder();
        sensorchannel.addSensors(SensorSourceService.Sensor.newBuilder().setSensorType(SensorType.SENSOR_DRIVING_STATUS_DATA).build());

        sensorchannel.addSensors(SensorSourceService.Sensor.newBuilder().setSensorType(SensorType.SENSOR_NIGHT_MODE).build());
        sensorchannel.addSensors(SensorSourceService.Sensor.newBuilder().setSensorType(SensorType.SENSOR_LOCATION).build());
        sensorchannel.addSensors(SensorSourceService.Sensor.newBuilder().setSensorType(SensorType.SENSOR_PARKING_BRAKE).build());


        channelDescriptior.setSensorSourceService(sensorchannel.build());
        channelDescriptior.setId(SENORCHANNEL);
        newdata.addServices(channelDescriptior.build());
        //Video
        channelDescriptior.clear();


        MediaSinkService.Builder avch = MediaSinkService.newBuilder();
        avch.setAvailableType(MediaCodecType.MEDIA_CODEC_VIDEO_H264_BP);
        avch.setDisplayType(DisplayType.DISPLAY_TYPE_MAIN);


        avch.addVideoConfigs(VideoConfiguration.newBuilder().setDensity(dpi).setFrameRate(VideoFrameRateType.VIDEO_FPS_30).setCodecResolution(ScreenCalc.getCodec(hires)).setHeightMargin(ScreenCalc.getHeightMargin(hires)).setWidthMargin(ScreenCalc.getWidthMargin(hires)));

        Log.d("CarSetup", "AVC Channel setup request from protobuf: " + avch.toString());
        channelDescriptior.setMediaSinkService(avch.build());
        channelDescriptior.setId(VIDEOCHANNEL);
        newdata.addServices(channelDescriptior.build());
        //System Audio - Mandatory!
        channelDescriptior.clear();
        avch.clear();
        avch.setAvailableType(MediaCodecType.MEDIA_CODEC_AUDIO_PCM);
        avch.setAudioType(AudioStreamType.AUDIO_STREAM_SYSTEM_AUDIO);
        avch.addAudioConfigs(AudioConfiguration.newBuilder().setNumberOfBits(16).setSamplingRate(16000).setNumberOfChannels(1).build());

        channelDescriptior.setMediaSinkService(avch.build());
        channelDescriptior.setId(AUDIO_SYSTEM);
        newdata.addServices(channelDescriptior.build());
        // TTS and MEDIA channel we can skip them to output the audio on the phone speaker or using A2DP

       if (!useBT) {
            //TTS
            channelDescriptior.clear();
            avch.clear();
            avch.setAvailableType(MediaCodecType.MEDIA_CODEC_AUDIO_PCM);
            avch.setAudioType(AudioStreamType.AUDIO_STREAM_GUIDANCE);
            avch.addAudioConfigs(AudioConfiguration.newBuilder().setNumberOfBits(16).setSamplingRate(16000).setNumberOfChannels(1).build());

            channelDescriptior.setMediaSinkService(avch.build());
            channelDescriptior.setId(AUDIO_TTS);
            newdata.addServices(channelDescriptior.build());
            //MEDIA
            channelDescriptior.clear();
            avch.clear();
            avch.setAvailableType(MediaCodecType.MEDIA_CODEC_AUDIO_PCM);
            avch.setAudioType(AudioStreamType.AUDIO_STREAM_MEDIA);
            avch.addAudioConfigs(AudioConfiguration.newBuilder().setNumberOfBits(16).setSamplingRate(48000).setNumberOfChannels(2).build());

            channelDescriptior.setMediaSinkService(avch.build());
            channelDescriptior.setId(AUDIO_MEDIA);
            newdata.addServices(channelDescriptior.build());
        }
        // Microphone input
        channelDescriptior.clear();
        MediaSourceService.Builder micch = MediaSourceService.newBuilder();
        micch.setAvailableType(MediaCodecType.MEDIA_CODEC_AUDIO_PCM);
        micch.setAudioConfig(AudioConfiguration.newBuilder().setNumberOfBits(16).setSamplingRate(16000).setNumberOfChannels(1).build());
        channelDescriptior.setMediaSourceService(micch.build());
        channelDescriptior.setId(MICCHANNEL);
        newdata.addServices(channelDescriptior.build());
        // MediaInfo Channel
        channelDescriptior.clear();

        channelDescriptior.setMediaPlaybackService(MediaPlaybackStatusService.newBuilder().build());
        channelDescriptior.setId(MEDIAINFO);
        newdata.addServices(channelDescriptior.build());

        android.util.Log.d("CAR-BUILD", newdata.toString());
        return new AAMessage(CONTROLCHANNEL, FLAG_ALL, ControlMessageType.MESSAGE_SERVICE_DISCOVERY_RESPONSE_VALUE, newdata);
    }


}
