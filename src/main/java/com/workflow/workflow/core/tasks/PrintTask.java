package com.workflow.workflow.core.tasks;


/**
 * General purpose task that prints a message — used in tests.
 */
public class PrintTask implements TaskFunction {
    private final String message;
    private final long sleepMs;

    public PrintTask(String message, long sleepMs) {
        this.message = message;
        this.sleepMs = sleepMs;
    }

    public PrintTask(String message) {
        this(message, 0);
    }

    @Override
    public void execute() throws Exception {
        if (sleepMs > 0) Thread.sleep(sleepMs);
        System.out.println("    " + message);
    }
}