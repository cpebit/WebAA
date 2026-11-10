package it.cpeb.teslaa.tasker.state;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;

import com.joaomgcd.taskerpluginlibrary.action.TaskerPluginRunnerActionNoOutput;
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput;
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResult;
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultSucess;

import it.cpeb.teslaa.TransporterService;
import kotlin.Unit;

public class WebaaStateRunner extends TaskerPluginRunnerActionNoOutput<WebaaStateInput> {
    @NonNull
    @Override
    public TaskerPluginResult<Unit> run(@NonNull Context context, @NonNull TaskerInput<WebaaStateInput> taskerInput) {
        boolean start = taskerInput.getRegular().state;

        Intent startWifiServiceIntent = new Intent(context, TransporterService.class);
        if(start){
            context.startForegroundService(startWifiServiceIntent);
        } else {
            context.stopService(startWifiServiceIntent);
        }

        return new TaskerPluginResultSucess<>();
    }
}
