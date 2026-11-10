package it.cpeb.teslaa.tethering;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.ConnectivityManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.NonNull;

import com.topjohnwu.superuser.ipc.RootService;

public class RootTetherService extends RootService implements Handler.Callback {

    private final static String TAG = "HU-Tether";
    static final int MSG_STARTAP = 1;
    static final int MSG_STOPAP = 2;

    private static MyOreoWifiManager mMyOreoWifiManager;

    @Override
    public boolean handleMessage(@NonNull Message message) {
        if (message.what == MSG_STARTAP) {
            tether(true);
            return false;
        }

        if (message.what == MSG_STOPAP) {
            tether(false);
            return false;
        }

        return false;
    }

    @Override
    public IBinder onBind(@NonNull Intent intent) {
        Handler h = new Handler(Looper.getMainLooper(), this);
        Messenger m = new Messenger(h);
        return m.getBinder();
    }

    @Override
    public boolean onUnbind(@NonNull Intent intent) {
        return false; // don't rebind
    }

    void tether(boolean turnOn){
        if (mMyOreoWifiManager == null) {
            mMyOreoWifiManager = new MyOreoWifiManager(this);
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
}
class RootTetherConnector implements ServiceConnection {

    private final static String TAG = "HU-Tether";

    private Messenger m;
    private final boolean autoStart;

    public RootTetherConnector(boolean autoStart) {
        this.autoStart = autoStart;
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        m = new Messenger(service);

        if (autoStart) {
            startTether();
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {}

    void startTether(){
        sendMsg(RootTetherService.MSG_STARTAP);
    }

    void stopTether(){
        sendMsg(RootTetherService.MSG_STOPAP);
    }

    private void sendMsg(int msgId){
        if (m == null)
            return;
        Message msg = Message.obtain(null, msgId);
        try {
            m.send(msg);
        } catch (RemoteException e) {
            Log.e(TAG, "Remote error", e);
        } finally {
            msg.recycle();
        }
    }
}

