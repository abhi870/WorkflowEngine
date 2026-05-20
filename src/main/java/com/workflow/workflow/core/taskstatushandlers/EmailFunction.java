package com.workflow.workflow.core.taskstatushandlers;

import com.workflow.workflow.core.tasks.TaskFunction;

public class EmailFunction implements TaskFunction {
    @Override
    public void execute() throws Exception {
        System.out.println("Sending email");
    }
}
