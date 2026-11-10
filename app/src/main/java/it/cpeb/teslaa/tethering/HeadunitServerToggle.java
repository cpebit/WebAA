package it.cpeb.teslaa.tethering;

import android.os.Build;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class HeadunitServerToggle {

    public static boolean StartStop(boolean enabled) {
        try {
            Process p;
            p = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(p.getOutputStream());
            os.writeBytes("dumpsys activity services com.google.android.projection.gearhead.companion.DeveloperHeadUnitNetworkService | grep \"(nothing)\" | wc -l; exit; \n");
            os.flush();
            p.waitFor();
            String res = readFully(p.getInputStream());
            Log.d("HeadunitServer", "Current status: " + res);
            if (res.equalsIgnoreCase("0"))
                return true;
            p.destroy();
            p = Runtime.getRuntime().exec("su");
            os = new DataOutputStream(p.getOutputStream());
            String command = "startservice";
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                command = "start-foreground-service";
            if (enabled)
                os.writeBytes("am " + command + " -n com.google.android.projection.gearhead/.companion.DeveloperHeadUnitNetworkService -a shutdown; am force-stop com.google.android.gms;\n");
            else
                os.writeBytes("am " + command + " -n com.google.android.projection.gearhead/.companion.DeveloperHeadUnitNetworkService; \n");
            os.writeBytes("exit\n");
            os.flush();
            p.waitFor();
            res = readFully(p.getInputStream());
            Log.d("HeadunitServer", " " + res);
            if (!enabled)
                Thread.sleep(2500);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static String readFully(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length = 0;
        while ((length = is.read(buffer)) != -1) {
            baos.write(buffer, 0, length);
        }
        return baos.toString("UTF-8");
    }
}
