package it.cpeb.teslaa.tasker.state;

import androidx.annotation.NonNull;

import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfig;
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfigHelper;
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfigHelperNoOutput;

public class WebaaStateHelper extends TaskerPluginConfigHelperNoOutput<WebaaStateInput, WebaaStateRunner> {
    public WebaaStateHelper(@NonNull TaskerPluginConfig<WebaaStateInput> config) {
        super(config);
    }

    @NonNull
    @Override
    public Class<WebaaStateInput> getInputClass() {
        return WebaaStateInput.class;
    }

    @NonNull
    @Override
    public Class<WebaaStateRunner> getRunnerClass() {
        return WebaaStateRunner.class;
    }
}
