package it.cpeb.teslaa.services;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import java.net.ServerSocket;

import it.cpeb.teslaa.R;
import it.cpeb.teslaa.helpers.Log;
import it.cpeb.teslaa.helpers.SettingConsts;
import it.cpeb.teslaa.sslhelpers.CertManager;
import it.cpeb.teslaa.sslhelpers.sslEngineBuilder;

public class JavaHTTPServer implements Runnable {


    Context context;

    // port to listen connection
    static final int PORT = 8080;

    private Thread httpThread = null;
    private ServerSocket serverConnect = null;

    private String TAG = "HTTP";


    // Client Connection via Socket Class


    public JavaHTTPServer(Context context) {
        this.context = context;

        httpThread = new Thread(this);
        httpThread.start();

/*
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    ServerSocket serverConnect = new ServerSocket(PORT);
                    while (true) {
                        new ResponseServer(serverConnect.accept(), context).start();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
*/

    }

    @Override
    public void run() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String apiHostname = prefs.getString(SettingConsts.API_HOSTNAME_KEY, SettingConsts.DEFAULT_API_HOSTNAME);

        // we manage our particular client connection
        try {
            serverConnect = sslEngineBuilder.getSSlContext(CertManager.getServerSslCert(context), "aa").getServerSocketFactory().createServerSocket(PORT + 1);
            // ServerSocket serverConnect = new ServerSocket(PORT);
            while (!serverConnect.isClosed()) {
                try {
                    new ResponseServer(serverConnect.accept(), context, apiHostname).start();
                } catch (Exception e) {
                    Log.e(TAG, "failed to handle http request", e);
                }
                // Log.d("WebServer","got an accept");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if(serverConnect != null){
            try{
                serverConnect.close();
            } catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    public void stop(){
        if(serverConnect != null){
            try {
                serverConnect.close();
            } catch (Exception e){
                Log.e(TAG, "failed to stop HTTP",e);
            }
        }
        httpThread.interrupt();
    }
}