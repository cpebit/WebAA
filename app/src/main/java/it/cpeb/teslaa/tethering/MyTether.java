package it.cpeb.teslaa.tethering;

import static android.content.Context.TETHERING_SERVICE;
import static android.content.Context.WIFI_SERVICE;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.TetheringManager;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.preference.PreferenceManager;

import com.topjohnwu.superuser.ipc.RootService;

import java.io.DataOutputStream;
import java.lang.reflect.Method;

public class MyTether {

    private static Context mContext;
    private static MyOreoWifiManager mMyOreoWifiManager;
    private final static String TAG = "HU-Tether";

    private static RootTetherConnector rootTetherConnector;

    public static void configureHotspot(String name, String password, Context context, boolean forced) {
        WifiConfiguration apConfig = new WifiConfiguration();
        apConfig.SSID = name;
        apConfig.preSharedKey = password;
        apConfig.allowedKeyManagement.clear();
        apConfig.allowedKeyManagement.set(4);

        if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean("hotspot_autoconfig", true) || forced)
            try {
                WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(WIFI_SERVICE);
                Method setConfigMethod = wifiManager.getClass().getMethod("setWifiApConfiguration", WifiConfiguration.class);
                boolean status = (boolean) setConfigMethod.invoke(wifiManager, apConfig);
                Log.d(TAG, "setWifiApConfiguration - success? " + status);
            } catch (Exception e) {
                Log.e(TAG, "Error in configureHotspot");
                e.printStackTrace();
            }
    }

    public static void startTether(final Context context, boolean enabled) {
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(WIFI_SERVICE);
        assert wifiManager != null;
        if (enabled && wifiManager != null)
            wifiManager.setWifiEnabled(false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mContext = context;
            hotspotOreo(enabled);
            return;
        }
        try {
            if (Build.VERSION.SDK_INT == 25) //broken tethering
            {
                try {
                    Process p; //Turn on USB data
                    p = Runtime.getRuntime().exec("su");
                    DataOutputStream os = new DataOutputStream(p.getOutputStream());
                    os.writeBytes("service call connectivity " + ((enabled) ? "24" : "25") + " i32 0; exit; \n");
                    os.flush();
                    p.waitFor();
                } catch (Exception e) {
                }
            }

            WifiManager wifimanager = (WifiManager) context.getApplicationContext().getSystemService(WIFI_SERVICE);
            WifiConfiguration wificonfiguration = null;
            try {
                wifimanager.setWifiEnabled(!enabled);
                Method method = wifimanager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
                method.invoke(wifimanager, wificonfiguration, enabled);
            } catch (Exception e) {
                e.printStackTrace();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    @RequiresApi(api = Build.VERSION_CODES.O)
    private static void hotspotOreo(boolean turnOn) {
        boolean rootWifi = PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean("rootwifi", false);
        if (rootWifi) {
            rootHotspotOreo(turnOn);
            return;
        }

        if (mMyOreoWifiManager == null) {
            mMyOreoWifiManager = new MyOreoWifiManager(mContext);
        }

        if (turnOn) {

            //this dont work
            MyOnStartTetheringCallback callback = new MyOnStartTetheringCallback() {
                @Override
                public void onTetheringStarted() {
                    Log.d(TAG, "Tethering Starterd");
                }

                @Override
                public void onTetheringFailed() {
                    Log.d(TAG, "Tethering failed");
                }
            };

            mMyOreoWifiManager.startTethering(callback, null);
        } else {
            mMyOreoWifiManager.stopTethering();

        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private static void rootHotspotOreo(boolean turnOn) {
        if(rootTetherConnector == null){
            rootTetherConnector = new RootTetherConnector(turnOn);
            Intent intent = new Intent(mContext, RootTetherService.class);
            RootService.bind(intent, rootTetherConnector);
        } else {
            if(turnOn){
                rootTetherConnector.startTether();
            } else {
                rootTetherConnector.stopTether();
            }
        }
    }
}
