package it.cpeb.teslaa.helpers;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Looper;
import android.widget.Toast;

import androidx.preference.PreferenceManager;

public class Messages {
    public static void show(String message, Context context, boolean force) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (force || !prefs.getBoolean("disable_notifications", false)) {
            if (Looper.myLooper() == null)
                Looper.prepare();
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        }
    }

    public static void show(String message, Context context) {
        show(message, context, false);
    }
}
