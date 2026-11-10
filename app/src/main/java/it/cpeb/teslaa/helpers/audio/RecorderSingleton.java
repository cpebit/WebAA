package it.cpeb.teslaa.helpers.audio;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

public class RecorderSingleton {

    private static final int FREQUENCY = 8000;

    public static RecorderSingleton instance = new RecorderSingleton();
    private AudioRecord recordInstance = null;
    private final int bufferSize;
    private boolean started = false;

    private RecorderSingleton() {
        bufferSize = AudioRecord.getMinBufferSize(FREQUENCY, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
    }

    public boolean init() {
        recordInstance = new AudioRecord(MediaRecorder.AudioSource.DEFAULT, FREQUENCY, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);
        return recordInstance.getState() != AudioRecord.STATE_UNINITIALIZED;
    }

    public int getBufferSize() {
        return bufferSize;
    }

    public boolean start() {
        if (started)
            return started;

        if (recordInstance != null && recordInstance.getState() != AudioRecord.STATE_UNINITIALIZED) {
            if (recordInstance.getRecordingState() != AudioRecord.RECORDSTATE_STOPPED) {
                recordInstance.stop();
            }
            recordInstance.release();
        }
        if (!init()) {
            return false;
        }
        recordInstance.startRecording();
        started = true;
        return true;
    }

    public int read(byte[] tempBuffer) {
        if (recordInstance == null) {
            return AudioRecord.ERROR_INVALID_OPERATION;
        }
        int ret = recordInstance.read(tempBuffer, 0, bufferSize);
        return ret;
    }

    public void stop() {
        if (recordInstance == null) {
            return;
        }
        recordInstance.stop();
        recordInstance.release();
    }
}