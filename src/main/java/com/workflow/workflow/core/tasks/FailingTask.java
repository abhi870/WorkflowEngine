package com.workflow.workflow.core.tasks;


/**
 * Always fails — used to test cascade and failure handling.
 */
public class FailingTask implements TaskFunction {
    private final String reason;

    public FailingTask(String reason) {
        this.reason = reason;
    }

    @Override
    public void execute() throws Exception {
        throw new RuntimeException(reason);
    }
}