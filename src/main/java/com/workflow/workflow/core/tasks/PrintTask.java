package com.workflow.workflow.core.tasks;


public class PrintTask implements TaskFunction {

    private String message = "Task executed";
    private long sleepMs = 0;

    public PrintTask() {
    }

    public PrintTask(String message) {
        this.message = message;
    }

    public PrintTask(String message, long sleepMs) {
        this.message = message;
        this.sleepMs = sleepMs;
    }

    public PrintTask setMessage(String message) {
        this.message = message;
        return this;
    }

    public PrintTask setSleepMs(long sleepMs) {
        this.sleepMs = sleepMs;
        return this;
    }

    @Override
    public void execute() throws Exception {
        if (sleepMs > 0) Thread.sleep(sleepMs);
        System.out.println("    " + message);
    }
}