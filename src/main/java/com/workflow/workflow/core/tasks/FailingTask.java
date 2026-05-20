package com.workflow.workflow.core.tasks;

/**
 * FailingTask
 * <p>
 * Always fails — used to test cascade and failure handling.
 * No-arg constructor required by TaskRegistry.
 */
public class FailingTask implements TaskFunction {

    private String reason = "Task failed";

    /**
     * Required by TaskRegistry — no-arg constructor
     */
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