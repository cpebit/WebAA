package it.cpeb.teslaa.helpers;

import static it.cpeb.teslaa.helpers.Constants.filename;

import android.content.Context;

import androidx.core.content.FileProvider;
import androidx.preference.PreferenceManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;

public class Log {
    private static boolean mSave;
    private static FileOutputStream outputstream;


    public static void openFile(Context context) {
        android.util.Log.d("HU", "openFile called");
        mSave = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("enabledebug", false);
        if (!mSave) {
            File file = new File(context.getFilesDir(), filename);
            if (file.exists())
                file.delete();
            return;
        }
        try {
            android.util.Log.d("HU", "Have permission, creating file...");
            File file = new File(context.getFilesDir(), filename);

            try {
                if (!file.exists())
                    file.createNewFile();

                Log.outputstream = context.openFileOutput(filename, Context.MODE_APPEND);
            } catch (Exception E) {
                android.util.Log.e("Logger", E.getLocalizedMessage());
            }
        } catch (Exception e) {
            android.util.Log.d("HU", "No permission");
        }

    }

    public static android.net.Uri getBugReport(Context context) { // ToDo change log location, save localy then email.

        try {


            File file = new File(context.getFilesDir(), filename);

            try {
                if (!file.exists())
                    return null;
                File logfile = context.getFileStreamPath(filename);
                return FileProvider.getUriForFile(context, "gb.xxy.hr", logfile);
            } catch (Exception E) {
                android.util.Log.e("Logger", E.getLocalizedMessage());
            }
        } catch (Exception e) {
            android.util.Log.d("HU", "No permission");
        }

        return null;
    }

    public static void close() {
        try {
            if (outputstream != null) {
                outputstream.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            outputstream = null;
        }
    }

    public static void d(String tag, String text) {
        if (mSave)
            save(tag, "D", text, "");
        android.util.Log.d(tag, text);
    }

    public static void e(String tag, String text) {
        e(tag, text, null);
    }

    public static void e(String tag, String text, Throwable tr) {
        if (mSave)
            save(tag, "E", text, android.util.Log.getStackTraceString(tr));
        android.util.Log.e(tag, text, tr);
    }

    public static void w(String tag, String text) {
        w(tag, text, null);
    }

    public static void w(String tag, String text, Throwable tr) {
        if (mSave)
            save(tag, "W", text, android.util.Log.getStackTraceString(tr));
        android.util.Log.e(tag, text, tr);
    }

    public static void i(String tag, String text) {
        i(tag, text, null);
    }

    public static void i(String tag, String text, Throwable tr) {
        if (mSave)
            save(tag, "I", text, android.util.Log.getStackTraceString(tr));
        android.util.Log.i(tag, text, tr);
    }

    private static void save(String tag, String type, String text, String stackTrace) {
        if (outputstream != null) {
            StringBuilder aux = new StringBuilder();
            aux.append(DateFormat.getDateTimeInstance().format(new Date()));
            aux.append(" ");
            aux.append(type);
            aux.append(" - ");
            aux.append(tag);
            aux.append(": ");
            aux.append(text);
            if (!stackTrace.equalsIgnoreCase("")) {
                aux.append("# StackTrace: ");
                aux.append(stackTrace);
            }
            try {
                outputstream.write(aux.toString().getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
