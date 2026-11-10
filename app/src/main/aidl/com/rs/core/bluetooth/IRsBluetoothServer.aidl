
package com.rs.core.bluetooth;
import android.net.Uri;



interface IRsBluetoothServer {

     void startDiscovery();
            void stopDiscovery();
           void SendData(in String data);
           boolean isDiscovering();
           void connectDevice(String mac,String name,String type);
           void disConnectDevice(String mac,String name,String type);
           String getConnectDevice();
           void playMusic();
           void stopMusic();
           void pauseMusic();
           void playPauseMusic();
           void nextMusic();
           void preMusic();
           void muteMusic();
           void unMuteMusic();
           boolean isMusicPlaying();
           String getMusicInfo();
           void answerPhone();
           void noAnswerPhone();
           void endPhone();
           void replayPhone();
           void placeCall(in String number);
           void putMsg(in String number);
           void resetBt();
           void downLoadPhoneBook();
           boolean getBtIsDownLoad();
           void SwitchVoice();
           void redia();
           void reqDiaCallLog();
           void reqMissedCallLog();
           void reqReceiveCallLog();
           void reqAllCallLog();
           void reqVolume(String style,String number);
           void openBluetooth();
            void closeBluetooth();

}
