package it.cpeb.teslaa.helpers.messageprocessors;

import android.util.Log;

import it.cpeb.teslaa.TransporterService;
import it.cpeb.teslaa.helpers.Util;


//import com.rs.core.bluetooth.IRsBluetoothServer;


public class BtMessage {
    TransporterService mTransporter;
    // private IRsBluetoothServer btService;
    private String phoneAddress;

    public BtMessage(TransporterService mTransporter) {
        this.mTransporter = mTransporter;
    }

    public byte[] processMessage(int flag, int msgtype, byte[] data) throws Exception {


        Log.d("BT-CHANNEL", "Message: " + msgtype + ", flag: " + flag + ", data: " + Util.bytesToHex(data));
/*
        if (msgtype == 32769) // Sensor Start Request
        {
           byte[] protodata = new byte[data.length - 2];
            System.arraycopy(data, 2, protodata, 0, data.length - 2);
            BluetoothPairingRequestMessage.BluetoothPairingRequest phonedata = BluetoothPairingRequestMessage.BluetoothPairingRequest.parseFrom(protodata);
            String phoneAddress = phonedata.getPhoneAddress();
            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(mTransporter).edit();
            editor.putString("lastphonemac", phoneAddress);
            editor.commit();
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();

            if (adapter != null) {
                boolean connected = false;
                for (BluetoothDevice device : adapter.getBondedDevices()) {
                    if (device.getAddress().equalsIgnoreCase(phoneAddress)) {
                        connected = true;
                        break;
                    }
                }

                if (!connected) {
                    final BluetoothDevice device = adapter.getRemoteDevice(phoneAddress);

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        device.createBond();
                    } else        //need to do reflection for older devices....
                    {
                        try {
                            device.getClass().getMethod("setPairingConfirmation", boolean.class).invoke(device, true);
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        } catch (InvocationTargetException e) {
                            e.printStackTrace();
                        } catch (NoSuchMethodException e) {
                            e.printStackTrace();
                        }
                    }
/*
                    Intent intent = new Intent(BluetoothDevice.ACTION_PAIRING_REQUEST);
                    intent.putExtra(EXTRA_DEVICE, device);
                    int PAIRING_VARIANT_PIN = 272;
                    intent.putExtra(BluetoothDevice.EXTRA_PAIRING_VARIANT, PAIRING_VARIANT_PIN);
                    mTransporter.getService().sendBroadcast(intent);
*/
                    /*String ACTION_PAIRING_REQUEST = "android.bluetooth.device.action.PAIRING_REQUEST";
                    Intent intent = new Intent(ACTION_PAIRING_REQUEST);
                    String EXTRA_DEVICE = "android.bluetooth.device.extra.DEVICE";
                    intent.putExtra(EXTRA_DEVICE, device);
                    String EXTRA_PAIRING_VARIANT = "android.bluetooth.device.extra.PAIRING_VARIANT";

                    int PAIRING_VARIANT_PIN = 2;
                    intent.putExtra(EXTRA_PAIRING_VARIANT, PAIRING_VARIANT_PIN);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    mTransporter.getService().startActivity(intent);*/

                    /*Method method = Class.forName("android.os.ServiceManager").getMethod("getService", String.class);
                    IBinder binder = (IBinder) method.invoke(null, "rs_bluetooth_service");
                    if(binder != null) {
                        IRsBluetoothServer test = IRsBluetoothServer.Stub.asInterface(binder);
                        test.connectDevice(phoneAddress,"OnePlus","A2DP");
                    }
                   this.phoneAddress=phoneAddress;*/
        //  mTransporter.encrypt(BluetoothPairingResponseMessage.BluetoothPairingResponse.newBuilder().setAlreadyPaired(false).setStatus(BluetoothPairingStatusEnum.BluetoothPairingStatus.Enum.OK).build().toByteArray(),BLUETOOTHCHANNEL,FLAG_ALL);
        //  }
        //   else
        //    mTransporter.encrypt(BluetoothPairingResponseMessage.BluetoothPairingResponse.newBuilder().setAlreadyPaired(true).setStatus(BluetoothPairingStatusEnum.BluetoothPairingStatus.Enum.OK).build().toByteArray(),BLUETOOTHCHANNEL,FLAG_ALL);


        // }


        // }
        return null;
    }


}