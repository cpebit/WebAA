package com.rs.core.mcu;
import android.net.Uri;
/** {@hide} */
 interface IRsServer {
        void ReqSetVolume(int sourceid);
        /**设置混音使能和音量 */
        void ReqMix(int volum,boolean b);
        /**发送按键命令 */
        void SetKeyCMD(byte deviceId,byte arg1,byte arg2);
        /**
           * 切换信号源
           * @param currentSource 目前信号源
           * @param targetSource 目标信号源
           * @param Mode 切换方式
           */
        void ReqSwitchSource(byte currentSource,byte Mode);
        void ReqPRadar(boolean isOpen);
        void ReqCurSource();
        void sendbytes(byte cmd, byte sorce, byte length,in byte[] param);
    void screanTouch(float x,float y);
    int getDataForType(int type);
    float getCurrentMcuVolume();
    void switchSource(in String packagename);
        String getSystemInfo(in String key);
        void setMixVoice(boolean isVoice);
        void setHomeKeyEnable(boolean disable);
        void setMixEnable(boolean isVoice);
        void setMixVolume(int value);
        String getCurrentPackage();
        void setBwmRelaysOpen(boolean open);
        void setBwmRelaysOpen2(boolean open, boolean bCtr3Rls);
        void setBwmRelaysNaviOpen(boolean open);
        void setBwmRelaysNaviOpen2(boolean open, boolean bCtr3Rls);
        void setControlDisplaysOpen(boolean open);
        void SetDevicePower(byte seek,byte flag);
        void setHost(boolean host);
        String getPhoeState();
}
