package it.cpeb.teslaa.tethering;


import static android.content.Context.TETHERING_SERVICE;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.TetheringManager;
import android.net.wifi.SoftApConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.Executor;

/**
 * Created by jonro on 19/03/2018.
 */

@RequiresApi(api = Build.VERSION_CODES.O)
public class MyOreoWifiManager {

    /**
     * From {@link ConnectivityManager}
     */
    private static final int TETHERING_WIFI = 0;

    private static final String TAG = MyOreoWifiManager.class.getSimpleName();

    private final Context mContext;

    private boolean originalWifiState = false;
    private static HandlerThread tetherThread =null;

    public MyOreoWifiManager(Context c) {
        mContext = c;
    }

    public void startTethering(MyOnStartTetheringCallback callback, Handler handler){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            startTetheringBaklava();
        } else {
            startTetheringOreo(callback, handler);
        }
    }

    public void startTetheringOreo(MyOnStartTetheringCallback callback, Handler handler) {
        if(tetherThread == null) {
            tetherThread = new HandlerThread("TetherThread");
            tetherThread.start();
        }
        if(handler == null) {
            handler = new Handler(tetherThread.getLooper());
        }

        try {
            WifiManager wifiManager = (WifiManager) mContext.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            originalWifiState = wifiManager.isWifiEnabled();
        } catch (Exception e){
            Log.e(TAG, "failed to get wifi state", e);
        }

        CallbackMaker cm = new CallbackMaker(mContext, callback);
        Class<?> mSystemCallbackClazz = cm.getCallBackClass();
        Object mSystemCallback = null;
        try {


            //  Constructor constructor = mSystemCallbackClazz.getDeclaredConstructor(callback.getClass());
            //  mSystemCallback = constructor.newInstance(callback);

            Constructor constructor = mSystemCallbackClazz.getDeclaredConstructor(int.class);
            mSystemCallback = constructor.newInstance(0);

        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

        ConnectivityManager manager = (ConnectivityManager) mContext.getApplicationContext().getSystemService(ConnectivityManager.class);
        Method method = null;
        Class callbackClass = null;
        try {
            try {
                callbackClass = Class.forName("android.net.ConnectivityManager$OnStartTetheringCallback");


            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }

            method = manager.getClass().getDeclaredMethod("startTethering", int.class, boolean.class, callbackClass, Handler.class);

            if (method == null) {
                Log.e(TAG, "startTetheringMethod is null");
            } else {
                method.invoke(manager, TETHERING_WIFI, false, mSystemCallback, handler);
            }
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }


    public void stopTethering(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA){
            stopTetheringBaklava();
        } else {
            stopTetheringOreo();
        }
    }

    public void stopTetheringOreo() {

        WifiManager wifiManager = (WifiManager) mContext.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wifiManager.setWifiEnabled(originalWifiState);

        ConnectivityManager manager = (ConnectivityManager) mContext.getApplicationContext().getSystemService(ConnectivityManager.class);

        try {
            Method method = manager.getClass().getDeclaredMethod("stopTethering", int.class);

            if (method == null) {
                Log.e(TAG, "stopTetheringMethod is null");
            } else {
                method.invoke(manager, TETHERING_WIFI);
            }
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.BAKLAVA)
    private void startTetheringBaklava(){
        TetheringManager manager = (TetheringManager) mContext.getSystemService(TETHERING_SERVICE);

        TetheringManager.TetheringRequest req =  new TetheringManager.TetheringRequest.Builder(TetheringManager.TETHERING_WIFI).build();

        TetheringManager.StartTetheringCallback callback = new TetheringManager.StartTetheringCallback() {
            @Override
            public void onTetheringFailed(int error) {
                Log.e(TAG, "tethering failed: " + error);
            }

            @Override
            public void onTetheringStarted() {
                Log.i(TAG, "tethering started");
            }
        };

        manager.startTethering(req, mContext.getMainExecutor(), callback);
    }

    @RequiresApi(api = Build.VERSION_CODES.BAKLAVA)
    private void stopTetheringBaklava(){
        TetheringManager manager = (TetheringManager) mContext.getSystemService(TETHERING_SERVICE);

        TetheringManager.TetheringRequest req =  new TetheringManager.TetheringRequest.Builder(TetheringManager.TETHERING_WIFI).build();

        TetheringManager.StopTetheringCallback callback = new TetheringManager.StopTetheringCallback() {
            @Override
            public void onStopTetheringFailed(int error) {
                Log.e(TAG, "stop tethering failed: " + error);
            }

            @Override
            public void onStopTetheringSucceeded() {
                Log.i(TAG, "stopped tethering");
            }
        };

        manager.stopTethering(req, mContext.getMainExecutor(), callback);
    }


}