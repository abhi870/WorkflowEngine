package com.workflow.workflow.core.tasks;


public class FailingTask implements TaskFunction {

    private String reason = "Task failed";

    public FailingTask() {
    }

    public FailingTask(String reason) {
        this.reason = reason;
    }

    public FailingTask setReason(String reason) {
        this.reason = reason;
        return this;
    }

    @Override
    public void execute() throws Exception {
        throw new RuntimeException(reason);
    }
}