package it.cpeb.teslaa.sslhelpers;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import it.cpeb.teslaa.helpers.Log;

public class CertUpdateWorker extends Worker {
    public static final String WORK_NAME = "CertUpdate";

    public CertUpdateWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            CertManager.updateServerSslCert(getApplicationContext());
            return Result.success();
        } catch (Exception e) {
            Log.e(WORK_NAME, "error during update", e);
            return Result.retry();
        }
    }
}
