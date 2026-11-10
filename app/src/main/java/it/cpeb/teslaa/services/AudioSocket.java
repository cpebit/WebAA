package it.cpeb.teslaa.services;

import android.content.Context;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.DefaultSSLWebSocketServerFactory;
import org.java_websocket.server.WebSocketServer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.net.ssl.SSLContext;

import it.cpeb.teslaa.sslhelpers.CertManager;
import it.cpeb.teslaa.sslhelpers.sslEngineBuilder;

public class AudioSocket extends WebSocketServer {


    private WebSocket connection;
    private SSLContext sslContext;
    private ByteArrayOutputStream audiobuffer = new ByteArrayOutputStream(8192 * 64);


    public AudioSocket(InetSocketAddress address, Context context) {
        super(address);
        setTcpNoDelay(true);
        try {
            sslContext = sslEngineBuilder.getSSlContext(CertManager.getServerSslCert(context), "aa");


            this.setWebSocketFactory(new DefaultSSLWebSocketServerFactory(sslContext));
        } catch (KeyStoreException | UnrecoverableKeyException | KeyManagementException |
                 IOException | NoSuchAlgorithmException | CertificateException e) {
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
        //JpegDecoder.toogleVideoFocus(false);
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {


    }


    @Override
    public void onMessage(WebSocket conn, String message) {


    }

    @Override
    public void onMessage(WebSocket conn, ByteBuffer message) {
        //  System.out.println("received ByteBuffer from "	+ conn.getRemoteSocketAddress());


    }

    @Override
    public void onError(WebSocket conn, Exception ex) {

        //  System.err.println("an error occurred on connection "  + ":" + ex);
    }


    @Override
    public void onStart() {
        //  System.out.println("server started successfully");
    }

    public void PushToBrowser(byte[] data, boolean buffer) {
        if (buffer) {
            if (audiobuffer.size() < 128 * 8192) {
                try {
                    audiobuffer.write(data);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else {
                try {
                    connection.send(audiobuffer.toByteArray());
                    audiobuffer.reset();
                } catch (Exception e) {
                }
            }
        } else {
            try {
                connection.send(data);
            } catch (Exception e) {
            }
        }

    }


}