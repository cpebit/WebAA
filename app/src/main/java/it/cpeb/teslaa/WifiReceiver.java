package it.cpeb.teslaa;

import static it.cpeb.teslaa.TransporterService.isRunning;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.preference.PreferenceManager;

import java.util.Set;


/**
 * Created by Emil on 21/05/2017.
 */

public class WifiReceiver extends BroadcastReceiver {

    public final static String ACTION_EXIT = "it.cpeb.teslaa.exit";

    @Override
    public void onReceive(final Context context, Intent intent) {
        Log.d("HU-RECEIVER", "Receiver fired..." + intent.getAction());
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        Set<String> selectedBluetoothMacs = sharedPreferences.getStringSet("selected_bluetooth_devices", null);


        Intent startWifiServiceIntent = new Intent(context, TransporterService.class);
        if (ACTION_EXIT.equalsIgnoreCase(intent.getAction())) {
            Log.d("HEREIVER", "Please exit the service");
            context.stopService(startWifiServiceIntent);
            return;
        }
        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        if (device == null) {
            return;
        }


        if (selectedBluetoothMacs != null && selectedBluetoothMacs.contains(device.getAddress())) {
            switch (intent.getAction()) {
                case BluetoothDevice.ACTION_ACL_CONNECTED:
                    android.util.Log.d("BluetoothReceiver", intent.getAction() + " want: " + selectedBluetoothMacs + ", got: " + device.getAddress());
                    context.startForegroundService(startWifiServiceIntent);
                    break;
                case BluetoothDevice.ACTION_ACL_DISCONNECTED:
                    isRunning = false;
                    break;

            }
        }

    }


}



