package it.cpeb.teslaa.activities;

import static it.cpeb.teslaa.helpers.Constants.FLAG_ALL;
import static it.cpeb.teslaa.helpers.Constants.MICCHANNEL;
import static it.cpeb.teslaa.helpers.messageprocessors.MicMessage.FREQUENCY;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.media.AudioDeviceInfo;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresPermission;
import androidx.core.app.ActivityCompat;

import org.greenrobot.eventbus.EventBus;

import java.nio.ByteBuffer;

import it.cpeb.teslaa.R;
import it.cpeb.teslaa.helpers.AAMessage;
import it.cpeb.teslaa.helpers.audio.AudioPlayer;

public class MicActivity extends Activity {

    public static boolean micRunning;
    private final static String TAG = "MicActivity";
    private AudioManager am;
    private AudioRecord audiorecorder;
    private boolean communicationDeviceSet = false;
    private BroadcastReceiver rec = new BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        @Override
        public void onReceive(Context context, Intent intent) {
            int state = intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, -1);
            Log.d(TAG, "Audio SCO state: " + state);

            if (AudioManager.SCO_AUDIO_STATE_CONNECTED == state) {
                unregisterReceiver(this);
                startRecording(null);
            }
        }
    };


    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if ("it.cpeb.teslaa.STOP".equalsIgnoreCase(intent.getAction())) {
            micRunning = false;
            try {
                if (audiorecorder != null)
                    audiorecorder.stop();
                Log.d(TAG, "Call Release");
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
            if (audiorecorder != null)
                audiorecorder.release();
            if (am != null) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
                    am.stopBluetoothSco();
            }


            finish();
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if ("it.cpeb.teslaa.STOP".equalsIgnoreCase(getIntent().getAction()))
            finish();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        } else {
            KeyguardManager keyguardManager = getSystemService(KeyguardManager.class);
            keyguardManager.requestDismissKeyguard(this, null);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    | WindowManager.LayoutParams.FLAG_FULLSCREEN
                    | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                    | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                    | WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON);

        }
        setContentView(R.layout.mic);
    }

    @Override
    protected void onResume() {

        super.onResume();
        if (micRunning)
            return;
        am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        micRunning = true;


        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO)) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 2);
            } else {
                AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppDialogTheme);

                builder.setTitle(getResources().getString(R.string.alert_permission_denied_title));
                builder.setMessage(getResources().getString(R.string.alert_need_mic));
                builder.setPositiveButton("OK", (dialog, id) -> {
                    dialog.dismiss();
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                    intent.setData(uri);
                    startActivity(intent);
                });
                builder.show();
            }
            return;
        }


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { // API 34+
            Log.d(TAG, "Attempting to set Bluetooth device using setCommunicationDevice");

            for (AudioDeviceInfo device : am.getAvailableCommunicationDevices()) {

                if (device.getType() == AudioDeviceInfo.TYPE_BLUETOOTH_SCO) {
                    boolean success = am.setCommunicationDevice(device);
                    Log.d(TAG, "Set communication device result: " + success);
                    if (success) {
                        communicationDeviceSet = true;
                        AudioPlayer.setRouting(am, true);
                        startRecording(device);
                    } else {
                        Log.e(TAG, "Failed to set Bluetooth SCO as communication device.");
                    }
                    break;
                }
            }
        } else {
            registerReceiver(rec, new IntentFilter(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED));
            Log.d(TAG, "starting bluetooth");
            am.startBluetoothSco();
        }


    }

    @Override
    protected void onStop() {
        super.onStop();
        try {
            unregisterReceiver(rec);
        } catch (Exception e) {
        }

    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private void startRecording(AudioDeviceInfo audioDeviceInfo) {
        int bufferSize = AudioRecord.getMinBufferSize(FREQUENCY, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);

        audiorecorder = new AudioRecord.Builder()
                .setAudioSource(MediaRecorder.AudioSource.UNPROCESSED)

                .setAudioFormat(new AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(16000)
                        .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                        .build())
                .setBufferSizeInBytes(bufferSize)
                .build();

        audiorecorder.setPreferredDevice(audioDeviceInfo);
        Thread t = new Thread(() -> {
            try {
                audiorecorder.startRecording();
                while (micRunning) {
                    byte[] tempBuffer = new byte[bufferSize];
                    audiorecorder.read(tempBuffer, 0, bufferSize);
                    sendMicData(tempBuffer);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (communicationDeviceSet && Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    Log.i(TAG, "clearing communication device");
                    am.clearCommunicationDevice();
                    AudioPlayer.setRouting(am, false);
                }
            }
        });
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();
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
}
