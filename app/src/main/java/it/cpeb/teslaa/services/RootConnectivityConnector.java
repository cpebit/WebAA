package it.cpeb.teslaa.services;

import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import it.cpeb.teslaa.helpers.SettingConsts;

public class RootConnectivityConnector implements ServiceConnection {

    private final static String TAG = "HU-Connectivity";

    private Messenger m;
    private Bundle connectivityConsts;
    private Context mContext;
    public RootConnectivityConnector(Context context) {
        mContext = context;
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        m = new Messenger(service);
        startService();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {}

    public void startService(){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);

        connectivityConsts = new Bundle();
        connectivityConsts.putString(SettingConsts.API_IP_KEY,
                prefs.getString(SettingConsts.API_IP_KEY, SettingConsts.DEFAULT_API_IP));
        connectivityConsts.putString(SettingConsts.API_HOSTNAME_KEY,
                prefs.getString(SettingConsts.API_HOSTNAME_KEY, SettingConsts.DEFAULT_API_HOSTNAME));

        sendMsg(RootConnectivityService.MSG_START, connectivityConsts);
    }

    public void stopService(){
        sendMsg(RootConnectivityService.MSG_STOP, connectivityConsts);
    }

    private void sendMsg(int msgId, @Nullable Bundle data){
        if (m == null)
            return;
        Message msg = Message.obtain(null, msgId);
        if(data != null){
            msg.setData(data);
        }

        try {
            m.send(msg);
        } catch (RemoteException e) {
            Log.e(TAG, "Remote error", e);
        } finally {
            msg.recycle();
        }
    }
}

