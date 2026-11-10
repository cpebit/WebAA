package it.cpeb.teslaa;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.VpnService;
import android.os.Bundle;
import android.provider.Settings;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.PreferenceManager;
import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import org.greenrobot.eventbus.EventBus;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import it.cpeb.teslaa.helpers.Log;
import it.cpeb.teslaa.sslhelpers.CertUpdateWorker;


public class MainActivity extends AppCompatActivity {
    private static final String TAG = "HUR-MAIN";
    private AlertDialog mDialog;


    @Override
    public void onResume() {
        super.onResume();


    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
        Log.d("HU-MAIN", "on Create");

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        int hires = Integer.parseInt(sp.getString("resolution", "1"));
        System.out.println("res: " + hires);
        /*if (hires==2)
            sp.edit().putString("resolution","1").commit();*/


        super.onCreate(savedInstanceState);


        setContentView(R.layout.activity_main);
        getVpnService();
        setupCertUpdater();

        // Set the toolbar as the action bar
        // On Android 15+, the notch completely messes everything up, this fixes it somehow
        Toolbar toolbar = findViewById(R.id.app_toolbar);
        setSupportActionBar(toolbar);
    }

    private void setupCertUpdater() {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.UNMETERED)
                .setRequiresBatteryNotLow(true)
                .build();

        PeriodicWorkRequest updateCertRequest =
                new PeriodicWorkRequest.Builder(CertUpdateWorker.class, 1, TimeUnit.DAYS)
                        .setConstraints(constraints)
                        .setBackoffCriteria(BackoffPolicy.LINEAR, Duration.ofHours(1))
                        .build();


        WorkManager.getInstance(getApplicationContext()).
                enqueueUniquePeriodicWork(CertUpdateWorker.WORK_NAME,
                        ExistingPeriodicWorkPolicy.KEEP,
                        updateCertRequest
                );
    }


    private void getVpnService() {

        if (!PreferenceManager.getDefaultSharedPreferences(this).getBoolean("usevpn", true))
            return;
        Intent vpnintent = VpnService.prepare(getApplicationContext());
        if (vpnintent != null) {
            startActivityForResult(vpnintent, 0);
        } else {
            onActivityResult(0, RESULT_OK, null);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("HU-MAIN", "on Destroy");

    }


    @Override
    protected void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);

    }


    @SuppressLint("NewApi")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && Settings.System.canWrite(this)) {
            Log.d("TAG", "MainActivity.CODE_WRITE_SETTINGS_PERMISSION success");
            //do your code
        }

        if (requestCode == 0 && resultCode != RESULT_OK) {
            getVpnService();
        }
        if (requestCode == 2 && resultCode != RESULT_OK) {

        }
    }


}