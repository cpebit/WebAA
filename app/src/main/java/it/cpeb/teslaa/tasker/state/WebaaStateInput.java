package it.cpeb.teslaa.tasker.state;

import com.joaomgcd.taskerpluginlibrary.input.TaskerInputField;
import com.joaomgcd.taskerpluginlibrary.input.TaskerInputRoot;

import it.cpeb.teslaa.R;

@TaskerInputRoot
public class WebaaStateInput {
    @TaskerInputField(key="State") public boolean state;

    public WebaaStateInput() {}

    public WebaaStateInput(boolean state) {
        this.state = state;
    }
}