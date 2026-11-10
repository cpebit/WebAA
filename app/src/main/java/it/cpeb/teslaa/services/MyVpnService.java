package it.cpeb.teslaa.services;

import static it.cpeb.teslaa.TransporterService.isRunning;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import androidx.preference.PreferenceManager;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.FileChannel;

import it.cpeb.teslaa.helpers.SettingConsts;
import it.cpeb.teslaa.helpers.Util;

public class MyVpnService extends VpnService {

    private Thread mThread;
    private String TAG="VPN";
    private ParcelFileDescriptor mInterface;
    //a. Configure a builder for the interface.
    Builder builder = new Builder();


    @Override
    public void onCreate() {
        super.onCreate();

    }

    // Services interface
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String vpnIp = prefs.getString(SettingConsts.API_IP_KEY, SettingConsts.DEFAULT_API_IP);

        // Start a new session by creating a new thread.
        mThread = new Thread(new Runnable() {
            @SuppressLint("NewApi")
            @Override
            public void run() {
                try {

                    mInterface = builder.setSession("MyVPNService")
                            .addAddress(vpnIp, 24)
                            .addDnsServer("8.8.8.8")
                            .addAllowedApplication("android.net.ConnectivityManager")

                            .addRoute("0.0.0.0", 24).establish();


                    DatagramChannel tunnel = DatagramChannel.open();

                    tunnel.connect(new InetSocketAddress("127.0.0.1", 8087));

                    protect(tunnel.socket());
                    Log.d("VpnService", "VPN Service running");

                    Intent intent2 = new Intent(getApplicationContext(), NewVpnService.class);
                    startService(intent2);


                    while (isRunning) {
                        Thread.sleep(100);

                    }

                } catch (Exception e) {
                    // Catch any exception
                    e.printStackTrace();
                } finally {
                    try {
                        if (mInterface != null) {
                            mInterface.close();
                            mInterface = null;
                        }
                    } catch (Exception e) {

                    }
                    stopSelf();
                }
            }

        }, "MyVpnRunnable");

        //start the service
        mThread.start();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        // TODO Auto-generated method stub
        Log.d("VpnService", "On Destroy was called");
        super.onDestroy();
    }

}
