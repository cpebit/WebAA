package it.cpeb.teslaa.helpers.audio;

import static it.cpeb.teslaa.helpers.Constants.AUDIO_MEDIA;
import static it.cpeb.teslaa.helpers.Constants.AUDIO_SYSTEM;
import static it.cpeb.teslaa.helpers.Constants.AUDIO_TTS;
import static it.cpeb.teslaa.helpers.Constants.VIDEOCHANNEL;

import android.media.AudioAttributes;
import android.media.AudioDeviceInfo;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Build;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AudioPlayer {
    public static HashMap<Byte, AudioTrack> audiochannels = new HashMap<Byte, AudioTrack>() {{

        put(AUDIO_SYSTEM, new AudioTrack.Builder().setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANT).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
                .setAudioFormat(new AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(16000).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build()).setBufferSizeInBytes(4 * 2048).build());

    }};

    private static final HashMap<Byte, ExecutorService> audioexecutor = new HashMap<Byte, ExecutorService>() {{

        put(AUDIO_SYSTEM, Executors.newSingleThreadExecutor());

    }};

    public static HashMap<Byte, Integer> audiosessions = new HashMap<Byte, Integer>() {{
        put(AUDIO_MEDIA, -1);
        put(AUDIO_SYSTEM, -1);
        put(AUDIO_TTS, -1);
        put(VIDEOCHANNEL, -1); //Should I keep it in the MediaDecoder? Easier to work with it from here....
    }};

    public static void play(final byte channel) {
        //audiosessions.put(channel, audiosessions.get(channel) + 1);
        audioexecutor.get(channel).submit(new Runnable() {
            @Override
            public void run() {
                audiochannels.get(channel).play();
            }
        });
    }

    public static void pause(final byte channel) {


        audioexecutor.get(channel).submit(new Runnable() {
            @Override
            public void run() {
                audiochannels.get(channel).pause();
                audiochannels.get(channel).flush();
            }
        });
    }

    public static void stop(final byte channel) {


    audiochannels.get(channel).stop();


    }

    public static void write(final byte channel, final byte[] data, final int from) {
        audioexecutor.get(channel).submit(new Runnable() {
            @Override
            public void run() {
                if (audiochannels.get(channel).getPlayState() == AudioTrack.PLAYSTATE_PLAYING)
                    audiochannels.get(channel).write(data, from, data.length - from);
            }
        });
    }

    public static void setRouting(AudioManager am, boolean sco) {
        audioexecutor.get(AUDIO_SYSTEM).submit(new Runnable() {
            @Override
            public void run() {


                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { // API 34+
                    AudioDeviceInfo selectedDevice = null;
                    AudioDeviceInfo genericDevice = null;
                    for (AudioDeviceInfo device : am.getAvailableCommunicationDevices()) {
                        if (sco && device.getType() == AudioDeviceInfo.TYPE_BLUETOOTH_SCO) {
                            selectedDevice = device;
                            break;
                        } else if (!sco && device.getType() == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP) {
                            selectedDevice = device;
                            break;
                        } else if (device.getType() == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER) {
                            genericDevice = device;
                        }
                    }

                    if (selectedDevice == null) {
                        selectedDevice = genericDevice;
                    }

                    if (selectedDevice != null) {
                        audiochannels.get(AUDIO_SYSTEM).setPreferredDevice(selectedDevice);
                    }
                }
            }});
    }

    public static void stopAll() {
        for (Map.Entry<Byte, AudioTrack> pair : audiochannels.entrySet()) {
            if (pair.getValue().getState() != AudioTrack.STATE_UNINITIALIZED)
                try {
                    pair.getValue().pause();
                    pair.getValue().flush();
                } catch (Exception e) {
                } // Don't care if it throws an error because audio track is not initialized
        }
    }

    public static void destroyAll() {
        for (Map.Entry<Byte, AudioTrack> pair : audiochannels.entrySet()) {
            pair.getValue().pause();
            pair.getValue().flush();
            pair.getValue().stop();
            pair.getValue().release();
        }
    }

    public static void startAll() {
        for (Map.Entry<Byte, AudioTrack> pair : audiochannels.entrySet()) {
            pair.getValue().play();
        }
    }

}
