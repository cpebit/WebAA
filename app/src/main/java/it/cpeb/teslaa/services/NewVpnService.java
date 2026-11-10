package it.cpeb.teslaa.services;

import static it.cpeb.teslaa.TransporterService.isRunning;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.FileChannel;

import it.cpeb.teslaa.helpers.Util;

public class NewVpnService extends VpnService {

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
        // Start a new session by creating a new thread.
        mThread = new Thread(new Runnable() {
            @SuppressLint("NewApi")
            @Override
            public void run() {
                try {
                    Log.d("VpnService2", "Starting second VPN");
                    mInterface = builder.setSession("MyVPNService_2")
                            .addAddress("94.23.203.112", 24)
                            .addDnsServer("8.8.8.8")
                            .addAllowedApplication("android.net.ConnectivityManager")

                            .addRoute("0.0.0.0", 0).establish();


                    DatagramChannel tunnel = DatagramChannel.open();

                    tunnel.connect(new InetSocketAddress("127.0.0.1", 8089));
                    protect(tunnel.socket());
                    Log.d("VpnService2", "Second VPN Service running");
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
