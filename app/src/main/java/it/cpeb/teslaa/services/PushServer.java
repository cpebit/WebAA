package it.cpeb.teslaa.services;


import static it.cpeb.teslaa.helpers.Constants.FLAG_ALL;
import static it.cpeb.teslaa.helpers.Constants.SENORCHANNEL;
import static it.cpeb.teslaa.helpers.Constants.VIDEOCHANNEL;
import static it.cpeb.teslaa.helpers.audio.AudioPlayer.audiosessions;
import static it.cpeb.teslaa.helpers.video.JpegDecoder.toogleVideoFocus;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.DefaultSSLWebSocketServerFactory;
import org.java_websocket.server.WebSocketServer;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.net.ssl.SSLContext;

import it.cpeb.teslaa.R;
import it.cpeb.teslaa.helpers.AAMessage;
import it.cpeb.teslaa.helpers.messageprocessors.InputMessage;
import it.cpeb.teslaa.helpers.video.JpegDecoder;
import it.cpeb.teslaa.proto.Ack;
import it.cpeb.teslaa.proto.LocationData;
import it.cpeb.teslaa.proto.MediaMessageId;
import it.cpeb.teslaa.proto.NightModeData;
import it.cpeb.teslaa.proto.PointerAction;
import it.cpeb.teslaa.proto.SensorBatch;
import it.cpeb.teslaa.proto.SensorMessageId;
import it.cpeb.teslaa.proto.TouchEvent;
import it.cpeb.teslaa.sslhelpers.CertManager;
import it.cpeb.teslaa.sslhelpers.sslEngineBuilder;


public class PushServer extends WebSocketServer {


    private WebSocket connection;
    private SSLContext sslContext;
    private static final byte[] pong={0,0,0,0,31};

    public PushServer(InetSocketAddress address, Context context) {
        super(address);
        setTcpNoDelay(true);
        try {
            sslContext = sslEngineBuilder.getSSlContext(CertManager.getServerSslCert(context), "aa");


            this.setWebSocketFactory(new DefaultSSLWebSocketServerFactory(sslContext));
        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (CertificateException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (UnrecoverableKeyException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        try {
            Thread.sleep(1000); // page refresh causes shit with SSL handshake happening too fast, seems to fix it.
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        this.connection = conn;
        System.out.println("new connection to " + conn.getRemoteSocketAddress());
        JpegDecoder.toogleVideoFocus(false);
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {

        System.out.println("closed " + conn.getRemoteSocketAddress() + " with exit code " + code + " additional info: " + reason);
        JpegDecoder.toogleVideoFocus(false);

    }



    final Handler handler = new Handler();
    final Runnable runnable = new Runnable() {
        public void run() {

            toogleVideoFocus(false);
        }
    };

    @Override
    public void onMessage(WebSocket conn, String message) {
       // System.out.println("received message from "	+ conn.getRemoteSocketAddress() + ": " + message);
        if (!message.isEmpty())
            try {
                JSONObject jsonObj = new JSONObject(message);
                String action = jsonObj.getString("action");
                if (action.equals("START")) {

                    System.out.println("start video");
                    try {
                        handler.removeCallbacks(runnable);
                    }
                    catch (Exception e){}

                    JpegDecoder.toogleVideoFocus(true);
                    return;
                    //  PushToBrowser();
                }


                if (action.equals("UP")) {
                    TouchEvent.Builder touchdata = TouchEvent.newBuilder();
                    touchdata.addPointerData(TouchEvent.Pointer.newBuilder().setX(jsonObj.getInt("X")).setY(jsonObj.getInt("Y")).setPointerId(0).build());
                    touchdata.setAction(PointerAction.ACTION_UP);
                    touchdata.setActionIndex(0);
                    InputMessage.sendInput(touchdata);
                    return;

                }

                if (action.equals("DRAG")) {
                    TouchEvent.Builder touchdata = TouchEvent.newBuilder();
                    touchdata.addPointerData(TouchEvent.Pointer.newBuilder().setX(jsonObj.getInt("X")).setY(jsonObj.getInt("Y")).setPointerId(0).build());
                    touchdata.setAction(PointerAction.ACTION_MOVED);
                    touchdata.setActionIndex(0);
                    InputMessage.sendInput(touchdata);
                    return;

                }

                if (action.equals("DOWN")) {


                    TouchEvent.Builder touchdata = TouchEvent.newBuilder();
                    touchdata.addPointerData(TouchEvent.Pointer.newBuilder().setX(jsonObj.getInt("X")).setY(jsonObj.getInt("Y")).setPointerId(0).build());
                    touchdata.setAction(PointerAction.ACTION_DOWN);
                    touchdata.setActionIndex(0);
                    InputMessage.sendInput(touchdata);
                    return;

                }
                if (action.equals("GPS")) {
                    SensorBatch.Builder sensordata = SensorBatch.newBuilder();
                    LocationData.Builder locationData = LocationData.newBuilder();


                    try {
                        locationData.setAccuracyE3((int) (jsonObj.getDouble("accuracy") * 1000));
                    } catch (JSONException e) {
                    }

                    try {
                        locationData.setLatitudeE7((int) (jsonObj.getDouble("latitude") * 10000000));
                    } catch (JSONException e) {
                    }
                    try {
                        locationData.setLongitudeE7((int) (jsonObj.getDouble("longitude") * 10000000));
                    } catch (JSONException e) {
                    }
                    try {
                        locationData.setAltitudeE2((int) (jsonObj.getDouble("altitude") * 100));
                    } catch (JSONException e) {
                    }
                    try {
                        locationData.setBearingE6((int) (jsonObj.getDouble("heading") * 1000000));
                    } catch (JSONException e) {
                    }
                    try {
                        locationData.setSpeedE3((int) (jsonObj.getDouble("speed") * 100));
                    } catch (JSONException e) {
                    }

                    sensordata.addLocationData(locationData);


                    AAMessage aaMessage = new AAMessage(SENORCHANNEL, FLAG_ALL, SensorMessageId.SENSOR_MESSAGE_BATCH_VALUE, sensordata);
                    EventBus.getDefault().post(aaMessage);
                    return;
                }
                if (action.equals("NIGHT")) {
                    SensorBatch.Builder sensordata = SensorBatch.newBuilder();
                    NightModeData.Builder night = NightModeData.newBuilder();
                    night.setNightMode(jsonObj.getBoolean("value"));
                    sensordata.addNightModeData(night);
                    AAMessage aaMessage = new AAMessage(SENORCHANNEL, FLAG_ALL, SensorMessageId.SENSOR_MESSAGE_BATCH_VALUE, sensordata);
                    EventBus.getDefault().post(aaMessage);
                    return;
                }
                if (action.equals("STOP")) {
                    toogleVideoFocus(false);

                     return;
                }
                if (action.equals("RELOAD")) {
                    Log.d("ACK","REload request from browser");
                    toogleVideoFocus(false);

                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    toogleVideoFocus(true);

                    return;
                }
                if (action.equals("ACK")) {

                    Ack.Builder ack = Ack.newBuilder();
                    ack.setSessionId(audiosessions.get(VIDEOCHANNEL));
                    ack.setAck(1);
                    AAMessage ackrsp = new AAMessage(VIDEOCHANNEL, FLAG_ALL, MediaMessageId.MEDIA_MESSAGE_ACK_VALUE, ack);
                    EventBus.getDefault().post(ackrsp);
                    
                }
                else if (action.equals("PING")) {

                    connection.send(pong);

                    try {
                        handler.removeCallbacks(runnable);
                    }
                    catch (Exception e){}
                    handler.postDelayed(runnable, 3000);
                }

                else
                {
                    Log.d("Browser console: ",jsonObj.toString());
                }


            } catch (JSONException e) {
                e.printStackTrace();
            }

        // PushToBrowser(lastimage);
        // connection.send(LastImage.getCompressed());
        // lastPing=System.currentTimeMillis();
    }

    @Override
    public void onMessage(WebSocket conn, ByteBuffer message) {
        //    System.out.println("received ByteBuffer from "	+ conn.getRemoteSocketAddress());

    }

    @Override
    public void onError(WebSocket conn, Exception ex) {

        //  System.err.println("an error occurred on connection "  + ":" + ex);
    }


    @Override
    public void onStart() {
        //  System.out.println("server started successfully");
    }

    public void PushToBrowser(byte[] data) {

        try {
            connection.send(data);

        } catch (Exception e) {
            //System.out.println(e.getMessage());
            //  toogleVideoFocus(false);
        }

    }


}