package it.cpeb.teslaa;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static androidx.core.app.NotificationCompat.EXTRA_NOTIFICATION_ID;
import static org.mockito.Mockito.withSettings;
import static it.cpeb.teslaa.WifiReceiver.ACTION_EXIT;
import static it.cpeb.teslaa.helpers.Constants.CONTROLCHANNEL;
import static it.cpeb.teslaa.helpers.Constants.FLAG_ALL;
import static it.cpeb.teslaa.helpers.Constants.SENORCHANNEL;
import static it.cpeb.teslaa.helpers.Constants.startNight;
import static it.cpeb.teslaa.tethering.MyTether.startTether;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.VpnService;
import android.net.wifi.WifiInfo;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.preference.PreferenceManager;

import com.topjohnwu.superuser.ipc.RootService;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONException;
import org.json.JSONObject;
import org.mockito.Mockito;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.Security;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLEngine;

import it.cpeb.teslaa.activities.MicActivity;
import it.cpeb.teslaa.helpers.AAMessage;
import it.cpeb.teslaa.helpers.MessageProcessor;
import it.cpeb.teslaa.helpers.Messages;
import it.cpeb.teslaa.helpers.audio.AudioPlayer;
import it.cpeb.teslaa.helpers.eventbuscom.Exit;
import it.cpeb.teslaa.helpers.streams.InOutStream;
import it.cpeb.teslaa.helpers.video.ScreenCalc;
import it.cpeb.teslaa.proto.ByeByeReason;
import it.cpeb.teslaa.proto.ByeByeRequest;
import it.cpeb.teslaa.proto.ControlMessageType;
import it.cpeb.teslaa.proto.NightModeData;
import it.cpeb.teslaa.proto.PingRequest;
import it.cpeb.teslaa.proto.SensorBatch;
import it.cpeb.teslaa.proto.SensorMessageId;
import it.cpeb.teslaa.services.AudioSocket;
import it.cpeb.teslaa.services.JavaHTTPServer;
import it.cpeb.teslaa.services.MyVpnService;
import it.cpeb.teslaa.services.PushServer;
import it.cpeb.teslaa.services.RootConnectivityConnector;
import it.cpeb.teslaa.services.RootConnectivityService;
import it.cpeb.teslaa.sslhelpers.CertManager;
import it.cpeb.teslaa.sslhelpers.NioSSL;
import it.cpeb.teslaa.sslhelpers.sslEngineBuilder;


public class TransporterService extends Service {

    static {
        Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME);
        // Confirm that positioning this provider at the end works for your needs!
        Security.addProvider(new BouncyCastleProvider());
    }


    public ParcelFileDescriptor mInterface;
    public static boolean isRunning = true;
    public static boolean isConnected = false;
    public static boolean videoReady;
    private final ExecutorService outThread = Executors.newSingleThreadExecutor();

    private static final String PACKAGE_NAME_ANDROID_AUTO_WIRELESS = "com.google.android.projection.gearhead";
    private static final String CLASS_NAME_ANDROID_AUTO_WIRELESS = "com.google.android.apps.auto.wireless.setup.service.impl.WirelessStartupActivity";

    private static final String PARAM_HOST_ADDRESS_EXTRA_NAME = "PARAM_HOST_ADDRESS";
    private static final String PARAM_SERVICE_PORT_EXTRA_NAME = "PARAM_SERVICE_PORT";


    private static String TAG = "HU-TransportService";
    private static final int IPTOS_LOWDELAY = 0x10;
    public NotificationManager mNotificationManager;
    public static int websockport;
    public Notification mynotification;

    public boolean isnightset = false;
    public PendingIntent pendingIntent;
    private final IBinder mBinder = new LocalBinder();
    private NotificationCompat.Builder navnotification;
    private NioSSL ssl;

    private InOutStream inoutstream;
    private Socket transportSocket;
    private AAMessage inmessage;
    private MessageProcessor messageProcessor;
    private Thread pollthread;

    private String day = null;
    public static PushServer surfaceSocket;
    public static AudioSocket mediaSocket;
    public static AudioSocket notificationSocket;

    private ServerSocket server;
    private boolean stopTether = true;
    private RootConnectivityConnector rootConnectivityConnector = null;

    private JavaHTTPServer webserver = null;

    //public static AudioRecord recorder=null;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "Service binder called");

        return mBinder;
    }

    @Override
    public void onRebind(Intent intent) {

        Log.d(TAG, "Service re-binded!");
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "Service unbided, no more clients");
        return true;
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onCreate() {
        super.onCreate();


        TAG = TAG + "_" + BuildConfig.VERSION_NAME;
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Log.d(TAG, "Service created!: ");
        EventBus.getDefault().register(TransporterService.this);


        createChannel("main_notification", "WebAA Service", "WebAA - Web Interface for Android Auto™");


        Intent exitIntent = new Intent(this, WifiReceiver.class);
        exitIntent.setAction(ACTION_EXIT);
        exitIntent.putExtra(EXTRA_NOTIFICATION_ID, 0);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            pendingIntent = PendingIntent.getBroadcast(this, 0, exitIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        else
            pendingIntent =
                    PendingIntent.getBroadcast(this, 0, exitIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        mynotification = new NotificationCompat.Builder(this, "main_notification")
                .setContentTitle("WebAA")
                .setContentText("Web interface for Android Auto")
                .setSmallIcon(R.drawable.ic_launcher_foreground)

                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(false)

                .addAction(
                        new NotificationCompat.Action.Builder(R.drawable.ic_exit,
                                getString(R.string.exit),
                                pendingIntent).build())

                .build();


        startForeground(1, mynotification);
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        int stored_width = sp.getInt("stored_width", 1180);
        int stored_height = sp.getInt("stored_height", 860);
        ScreenCalc.SetScreenCalc(stored_width, stored_height);


    }

    private void rootConnectivityServer(boolean turnOn) {
        if (rootConnectivityConnector == null) {
            rootConnectivityConnector = new RootConnectivityConnector(this);
            Intent intent = new Intent(this, RootConnectivityService.class);
            RootService.bind(intent, rootConnectivityConnector);
        } else {
            if (turnOn) {
                rootConnectivityConnector.startService();
            } else {
                rootConnectivityConnector.stopService();
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "On Start Command");

        super.onStartCommand(intent, flags, startId);
        isRunning = true;
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        int hires = Integer.parseInt(sp.getString("resolution", "1"));
       /* if (hires==2)
            sp.edit().putString("resolution","1").commit();*/

        if (intent == null) {
            stopForeground(true);
            stopSelf();
            return START_NOT_STICKY;
        }

        if (ACTION_EXIT.equals(intent.getAction())) {
            stopForeground(true);
            stopSelf();
            return START_NOT_STICKY;
        }
        if (isConnected)
            return START_STICKY;

        // refresh cert if needed
        SynchronousQueue<Boolean> sslUpdateQueue = new SynchronousQueue<>();
        Context ctx = this;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    CertManager.updateServerSslCert(ctx);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } finally {
                    try {
                        sslUpdateQueue.put(true);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }).start();

        try {
            sslUpdateQueue.poll(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Log.e(TAG, "failed to updated SSL cert within 5 seconds");
        }

        Random r = new Random();
        websockport = r.nextInt(9998 - 8081) + 8081;

        Log.d(TAG, "Use VPN: " + sp.getBoolean("usevpn", true));
        if (sp.getBoolean("usevpn", true)) {
            VpnService.prepare(getApplicationContext());
            Intent intent1 = new Intent(this, MyVpnService.class);
            intent1.putExtra("fd", mInterface);
            startService(intent1);
        }

        surfaceSocket = new PushServer(new InetSocketAddress(websockport), getApplicationContext());
        mediaSocket = new AudioSocket(new InetSocketAddress(websockport + 1), getApplicationContext());
        notificationSocket = new AudioSocket(new InetSocketAddress(websockport + 2), getApplicationContext());

        try {
            webserver = new JavaHTTPServer(getApplicationContext());
        } catch (Exception e) {
            Log.e(TAG, "failed to setup http server", e);
        }
        surfaceSocket.start();
        mediaSocket.start();
        notificationSocket.start();

        startTether(getApplicationContext(), true);
        connect(null);

        Log.d(TAG, "Use HTTP Connectivity: " + sp.getBoolean("rootconnectivity", true));
        if (sp.getBoolean("rootconnectivity", true)) {
            rootConnectivityServer(true);
        }


        Network ns;

        Network fakeNetwork = null;
        try {
            fakeNetwork = Mockito.mock(Network.class, withSettings().useConstructor(99999));
        } catch (Exception e) {
        }
        if (fakeNetwork != null) {
            Parcel p = Parcel.obtain();
            fakeNetwork.writeToParcel(p, 0);
            p.setDataPosition(0);
            ns = Network.CREATOR.createFromParcel(p);
        } else {
            ConnectivityManager connectivity = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            ns = connectivity.getActiveNetwork();
        }

        Intent androidAutoWirelessIntent = new Intent();
        WifiInfo wifiinfo = null;

        try {
            Class<?> cl = Class.forName("android.net.wifi.WifiInfo");
            wifiinfo = (WifiInfo) cl.newInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }


        Log.d(TAG, "Wifi info: " + wifiinfo);
        androidAutoWirelessIntent.setClassName(PACKAGE_NAME_ANDROID_AUTO_WIRELESS, CLASS_NAME_ANDROID_AUTO_WIRELESS);
        androidAutoWirelessIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        androidAutoWirelessIntent
                .putExtra(PARAM_HOST_ADDRESS_EXTRA_NAME, "127.0.0.1")
                .putExtra(PARAM_SERVICE_PORT_EXTRA_NAME, 5288)
                .putExtra("wifi_info", wifiinfo)
                .putExtra("PARAM_SERVICE_WIFI_NETWORK", ns);
        try {
            startActivity(androidAutoWirelessIntent);
        } catch (Exception e) {
            Toast.makeText(this, getText(R.string.noaa), Toast.LENGTH_LONG).show();
            stopSelf();
            return START_NOT_STICKY;
        }
        System.out.println("Start intent sent");


        int car = Integer.parseInt(sp.getString("car_type", "0"));


        return START_STICKY;
    }


    public void updateNight(boolean night) {
        isnightset = night;
        SensorBatch.Builder sensordata = SensorBatch.newBuilder();
        sensordata.addNightModeData(NightModeData.newBuilder().setNightMode(night).build());
        AAMessage aaMessage = new AAMessage(SENORCHANNEL, FLAG_ALL, SensorMessageId.SENSOR_MESSAGE_BATCH_VALUE, sensordata);
        EventBus.getDefault().post(aaMessage);
    }

    @Override
    public void onDestroy() {
        try {
            if (mInterface != null) {
                mInterface.close();
                mInterface = null;
            }
        } catch (Exception e) {

        }
        android.util.Log.d(TAG, android.util.Log.getStackTraceString(new Exception()));
        AAMessage aaMessage = new AAMessage(CONTROLCHANNEL, FLAG_ALL, ControlMessageType.MESSAGE_BYEBYE_REQUEST_VALUE, ByeByeRequest.newBuilder().setReason(ByeByeReason.USER_SELECTION));
        EventBus.getDefault().post(aaMessage);


        isRunning = false;
        isConnected = false;

        try {
            surfaceSocket.stop();
            mediaSocket.stop();
            notificationSocket.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            webserver.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }

        EventBus.getDefault().unregister(this);
        if (stopTether)
            startTether(this, false);
        if (rootConnectivityConnector != null)
            rootConnectivityConnector.stopService();


        Log.d(TAG, "On Destroyed has been called, cleaning up...");

        try {
            AudioPlayer.destroyAll();
        } catch (Exception e) {
        }

        try {
            transportSocket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            server.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            pollthread.interrupt();
        } catch (Exception e) {
            e.printStackTrace();
        }

        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).cancelAll();
        stopForeground(true);

        super.onDestroy();

    }


    public void createChannel(String id, CharSequence name, String description) {
        int importance = NotificationManager.IMPORTANCE_LOW;
        NotificationChannel mChannel = new NotificationChannel(id, name, importance);
        // Configure the notification channel.
        mChannel.setDescription(description);
        mChannel.setShowBadge(false);
        mChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        mNotificationManager.createNotificationChannel(mChannel);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessage(Exit exit) throws IOException {
        //need to restart the service. should be enought to stop aa only and restart it but this is easier

        stopTether = false;
        Intent intent = new Intent(getApplicationContext(), TransporterService.class);
        PendingIntent pendingIntent = PendingIntent.getService(this, 1, intent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_ONE_SHOT);

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alarmManager.set(AlarmManager.RTC_WAKEUP, SystemClock.elapsedRealtime() + 5000, pendingIntent);
        stopForeground(true);
        stopSelf();
    }

    @Subscribe(threadMode = ThreadMode.POSTING)
    public void onMessage(final AAMessage aaMessage) {
        if (isConnected)
            outThread.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        byte[] aux = aaMessage.getFormated(ssl);
                        inoutstream.write(aux);


                    } catch (Exception e) {
                        Log.e(TAG, "" + e.getMessage(), e);
                        e.printStackTrace();
                    }
                }
            });
    }


    public class LocalBinder extends Binder {
        public TransporterService getService() {
            return TransporterService.this;
        }
    }


    private void connect(final String ip) {
        new Thread(new Runnable() {
            @Override
            public void run() {

                try {

                    server = new ServerSocket(5288);
                    server.setReuseAddress(true);
                    //transportSocket = new Socket(); //need to remove this, but currently transportSocket is checked if empty or not
                    Log.d(TAG, "Waiting for AA connection");
                    transportSocket = server.accept();
                    Log.d(TAG, "AA Connected");
                    //transportSocket.connect(new InetSocketAddress(ip, 5277), 4000);
                    connected();
                } catch (IOException e) {
                    e.printStackTrace();


                    try {
                        JSONObject obj = new JSONObject();
                        obj.put("type", "error");
                        obj.put("message", getString(R.string.cantconnect));
//                        mediaStatusSocket.PushToBrowser(obj.toString());

                    } catch (JSONException jsonException) {
                        jsonException.printStackTrace();
                    }


                    Messages.show(e.getMessage(), TransporterService.this);
                }
            }
        }).start();
    }

    private void connected() throws IOException {

        transportSocket.setSoTimeout(10000);
        transportSocket.setTcpNoDelay(true);
        transportSocket.setKeepAlive(true);
        transportSocket.setReuseAddress(true);
        transportSocket.setTrafficClass(IPTOS_LOWDELAY);
        inoutstream = new InOutStream(transportSocket.getInputStream(), transportSocket.getOutputStream());

        pollthread = new Thread(new pollThread());
        pollthread.start();
    }

    private void sendPingToPhone() {
        PingRequest.Builder ping = PingRequest.newBuilder();
        ping.setTimestamp(System.currentTimeMillis());
        EventBus.getDefault().post(new AAMessage(CONTROLCHANNEL, FLAG_ALL, ControlMessageType.MESSAGE_PING_REQUEST_VALUE, ping));
        final Handler handler = new Handler(getMainLooper());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isRunning) {
                    sendPingToPhone();
                }
            }
        }, 2000);
    }


    class pollThread implements Runnable {
        public void run() {
            Log.d(TAG, "Thread started");
            Looper.prepare();
            try {

                SSLEngine engine = sslEngineBuilder.Builder(getBaseContext());
                Log.d(TAG, "Send Car hello: ");
                inoutstream.write(new byte[]{0, 3, 0, 6, 0, 1, 0, 1, 0, 6}, 500);
                Log.d(TAG, "Read phone hello: ");
                inoutstream.read(2000);
                Log.d(TAG, "Phone hello: ");
                ssl = new NioSSL(engine, inoutstream, true);
                messageProcessor = new MessageProcessor(getBaseContext());
                Log.d(TAG, "Sending cleartext");
                inoutstream.write(new byte[]{0, 3, 0, 4, 0, 4, 8, 0}); // Auth Complete, must be sent in plaintext

            } catch (Exception e) {
                e.printStackTrace();
                try {
                    transportSocket.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                } catch (NullPointerException e1) {
                    e1.printStackTrace();
                    Toast.makeText(TransporterService.this, "Error initiating connection, if you are using USB connection, try to un-plug and replug your phone.", Toast.LENGTH_LONG).show();
                }
                if (e.getMessage() == null)
                    Toast.makeText(TransporterService.this, "Unknown error. If you are trying to connect over Wifi, probably the server is stuck.\nTry force closing Google Play Services on the phone.", Toast.LENGTH_LONG).show();
                else
                    Toast.makeText(TransporterService.this, e.getMessage(), Toast.LENGTH_LONG).show();

                return;
            }
            isConnected = true;


            sendPingToPhone();
            try {
                while (isRunning) {
                    inmessage = new AAMessage(inoutstream.read(), ssl);
                    messageProcessor.process(inmessage);
                }
                byte[] cmd_buf = {0, 0x0f, 0x08, 1}; //ByeBye
                inoutstream.write(new AAMessage(CONTROLCHANNEL, FLAG_ALL, 0, cmd_buf).getFormated(ssl));
            } catch (Exception e) {
                Log.e(TAG, "Error reading: " + e.getMessage(), e);
                e.printStackTrace();

            } finally {
                videoReady = false;
                // EventBus.getDefault().post(new Exit(false));

                Intent intent = new Intent(TransporterService.this, MicActivity.class);
                intent.setFlags(FLAG_ACTIVITY_NEW_TASK);
                intent.setAction("it.cpeb.teslaa.STOP");
                startActivity(intent);

                stopSelf();
                stopForeground(true);

            }
        }

    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessage(String aaMessage) {

        switch (aaMessage) {

            case startNight:
                day = getResources().getString(R.string.daynight);
                updateNight(!day.equalsIgnoreCase("day"));
                break;

        }
    }
}