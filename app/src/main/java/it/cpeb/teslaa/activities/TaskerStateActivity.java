package it.cpeb.teslaa.activities;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatCheckBox;

import com.joaomgcd.taskerpluginlibrary.SimpleResult;
import com.joaomgcd.taskerpluginlibrary.SimpleResultError;
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfig;
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput;
import com.joaomgcd.taskerpluginlibrary.input.TaskerInputInfos;

import it.cpeb.teslaa.R;
import it.cpeb.teslaa.helpers.Log;
import it.cpeb.teslaa.tasker.state.WebaaStateHelper;
import it.cpeb.teslaa.tasker.state.WebaaStateInput;


public class TaskerStateActivity extends AppCompatActivity implements TaskerPluginConfig<WebaaStateInput> {
    protected WebaaStateHelper helper = new WebaaStateHelper(this);
    private AppCompatCheckBox stateCheckbox = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LayoutInflater inflater = getLayoutInflater();

        View view = inflater.inflate(R.layout.activity_tasker_state, null);

        stateCheckbox = view.findViewById(R.id.checkboxSet);

        setContentView(view.getRootView());

        helper.onCreate();

        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                onFinish();
            }
        };

        getOnBackPressedDispatcher().addCallback(this, callback);

    }

    @NonNull
    @Override
    public Context getContext() {
        return getApplicationContext();
    }

    @Override
    public void assignFromInput(@NonNull TaskerInput<WebaaStateInput> taskerInput) {
        stateCheckbox.setChecked(taskerInput.getRegular().state);
    }

    @NonNull
    @Override
    public TaskerInput<WebaaStateInput> getInputForTasker() {
        WebaaStateInput input = new WebaaStateInput(stateCheckbox.isChecked());
        return new TaskerInput<>(input, new TaskerInputInfos());
    }

    private boolean onFinish() {
        SimpleResult res = helper.onBackPressed();
        if(res instanceof SimpleResultError){
            Log.e("TASKER", "invalid settings: " + ((SimpleResultError) res).getMessage());
        } else {
            return res.getSuccess();
        }
        return false;
    }
}
