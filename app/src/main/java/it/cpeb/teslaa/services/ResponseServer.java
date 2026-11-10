package it.cpeb.teslaa.services;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.preference.PreferenceManager;

import org.greenrobot.eventbus.EventBus;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Date;
import java.util.StringTokenizer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import it.cpeb.teslaa.BuildConfig;
import it.cpeb.teslaa.TransporterService;
import it.cpeb.teslaa.helpers.ReplacingInputStream;
import it.cpeb.teslaa.helpers.eventbuscom.Exit;
import it.cpeb.teslaa.helpers.video.ScreenCalc;


public class ResponseServer extends Thread {
    public static BlockingQueue<byte[]> nalBuffers = new ArrayBlockingQueue<>(128);
    private final Context context;
    private final Socket connect;

    private final String DEFAULT_FILE = "index.html";

    private final String mApiHostname;

    public ResponseServer(Socket socket, Context context, String apiHostname) {
        this.connect = socket;

        this.context = context;
        mApiHostname = apiHostname;

    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public void run() {
        super.run();

        //Log.d("WebServer", "connection accepted");
        BufferedReader in = null;
        PrintWriter out = null;
        BufferedOutputStream dataOut = null;
        String fileRequested = null;

        try {
            // we read characters from the client via input stream on the socket
            in = new BufferedReader(new InputStreamReader(connect.getInputStream()));
            // we get character output stream to client (for headers)
            out = new PrintWriter(connect.getOutputStream());
            // get binary output stream to client (for requested data)
            dataOut = new BufferedOutputStream(connect.getOutputStream());

            // get first line of the request from the client
            String input = in.readLine();
            if (input == null)
                return;
            Log.d("WebServer", "Input: " + input);
            // we parse the request with a string tokenizer
            StringTokenizer parse = new StringTokenizer(input);
            String method = parse.nextToken().toUpperCase(); // we get the HTTP method of the client
            // we get file requested
            fileRequested = parse.nextToken().toLowerCase();
            String originalFileRequested = fileRequested;


            if (method.equals("GET")) {
                if (fileRequested.startsWith("/getsocketport")) {
                    String[] params = fileRequested.split("\\?");
                    params = params[1].split("&");

                    int width = Integer.parseInt(params[0].split("=")[1]);
                    int height = Integer.parseInt(params[1].split("=")[1]);


                    Log.d("Screen", "Values from car: " + width + "x" + height);
                    SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(context);
                    int stored_width = SP.getInt("stored_width", 0);
                    int stored_height = SP.getInt("stored_height", 0);
                    boolean showdebug = SP.getBoolean("showdebug", false);
                    boolean usebt = SP.getBoolean("useBT", true);

                    if (stored_width != width || stored_height != height) {
                        SharedPreferences.Editor editor = SP.edit();
                        editor.putInt("stored_width", width).putInt("stored_height", height).apply();


                    }


                    out.println("HTTP/1.1 200 OK");
                    out.println("Server: HUR : 1.0");
                    out.println("Date: " + new Date());

                    out.println("Content-type: text/html");
                    out.println("Access-Control-Allow-Origin: * ");
                    out.println(); // blank line between headers and content, very important !
                    out.flush(); // flush character output stream buffer
                    int hires = Integer.parseInt(SP.getString("resolution", "1"));
                    String resp = "{\"width\":" + (ScreenCalc.getAAScreenWidthFor(hires) - ScreenCalc.getWidthMargin(hires)) + ",\"height\":" + (ScreenCalc.getAAScreenHeightFor(hires) - ScreenCalc.getHeightMargin(hires)) + ",\"port\":" + TransporterService.websockport + ",\"resolution\":" + hires + ",\"buildversion\":" + BuildConfig.VERSION_CODE + ",\"usebt\":" + usebt + ",\"debug\":" + showdebug + "}";
                    if (stored_width != width || stored_height != height) {
                        resp = "{\"wrongresolution\":true}";
                    }
                    dataOut.write(resp.getBytes());
                    dataOut.flush();

                    if (stored_width != width || stored_height != height)
                        EventBus.getDefault().post(new Exit(true));
                } else {
                    if (fileRequested.endsWith("/")) {
                        fileRequested = DEFAULT_FILE;
                    }

                    fileRequested = fileRequested.replace("/", "");
                    fileRequested = fileRequested.split("\\.")[0];


                    int resid = context.getResources().getIdentifier(fileRequested, "raw", context.getPackageName());
                    Log.d("WebServer", "Getting file: " + fileRequested + ", resource id: " + resid);
                    if (resid == 0) {
                        out.println("HTTP/1.1 404 File Not Found");
                        out.println("Server: Java HTTP Server from SSaurel : 1.0");
                        out.println("Date: " + new Date());

                        out.println(); // blank line between headers and content, very important !
                        out.flush(); // flush character output stream buffer


                    } else {
                        InputStream insRaw = context.getResources().openRawResource(resid);
                        InputStream ins = new ReplacingInputStream(insRaw, "${API_URL}", mApiHostname);

                        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                        int size = 0;

                        byte[] buffer = new byte[1024];
                        while ((size = ins.read(buffer, 0, 1024)) >= 0) {
                            outputStream.write(buffer, 0, size);
                        }

                        // send HTTP Headers
                        out.println("HTTP/1.1 200 OK");
                        out.println("Server: HUR : 1.0");
                        out.println("Date: " + new Date());
                        if(originalFileRequested.endsWith(".js")){
                            out.println("Content-Type: application/javascript");
                        } else {
                            out.println("Content-type: text/html");
                        }

                        out.println("Content-length: " + outputStream.size());
                        out.println("Access-Control-Allow-Origin: * ");
                        out.println(); // blank line between headers and content, very important !
                        out.flush(); // flush character output stream buffer

                        dataOut.write(outputStream.toByteArray(), 0, outputStream.size());
                        dataOut.flush();
                    }
                }
            }
        } catch (FileNotFoundException fnfe) {

            System.err.println("Error with file not found exception : ");


        } catch (IOException ioe) {
            System.err.println("Server error : " + ioe);
        } catch (Exception e) {
            System.err.println("Exception : " + e);
        } finally {
            try {
                in.close();
                out.close();
                dataOut.close();
                connect.close(); // we close socket connection
            } catch (Exception e) {
                System.err.println("Error closing stream : " + e.getMessage());
            }


        }


    }
}
