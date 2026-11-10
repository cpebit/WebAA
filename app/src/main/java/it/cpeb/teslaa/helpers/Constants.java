package it.cpeb.teslaa.helpers;

public class Constants {
    public final static byte CONTROLCHANNEL = 0;
    public final static byte INPUTCHANNEL = 1;
    public final static byte SENORCHANNEL = 2;
    public final static byte VIDEOCHANNEL = 3;
    public final static byte MICCHANNEL = 4;
    public final static byte AUDIO_MEDIA = 5;
    public final static byte AUDIO_SYSTEM = 6;
    public final static byte AUDIO_TTS = 7;
    public final static byte BLUETOOTHCHANNEL = 8;
    public final static byte MEDIAINFO = 9;
    public final static byte NAVINFO = 10;

    public final static byte FLAG_FIRST = 1;
    public final static byte FLAG_MIDDLE = 0;
    public final static byte FLAG_LAST = 2;
    public final static byte FLAG_ALL = 3; //ToDo check usage!


    public final static byte FLAG_CONTROL = 4; //ToDo check usage!
    public final static byte FLAG_SPECIFIC = 0;

    public final static byte FLAG_ENCRYPTED = 0x08;
    public final static byte FLAG_PLAIN = 0;

    //public final static byte FLAG_ALL = 0x0b;
    public final static String exitreceiver = "gb.xxy.hr.unregister.receiver";
    public final static String startGPS = "gb.xxy.hr.startGPS";
    public final static String startListener = "gb.xxy.hr.startListener";
    public final static String startSpeed = "gb.xxy.hr.startSpeed";
    public final static String startNight = "gb.xxy.hr.startNight";
    public final static String startBtService = "gb.xxy.hr.startBTService";
    public final static String filename = "hur.log";
    public final static int TOUCHPAD_HEIGHT = 240;
    public final static int TOUCHPAD_WIDTH = 240;
}
