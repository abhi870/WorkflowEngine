package com.workflow.workflow.core.tasks;

import java.util.concurrent.atomic.AtomicInteger;

public class FailOnceThenSucceedTask implements TaskFunction {

    private final String name;
    private final AtomicInteger callCount = new AtomicInteger(0);

    public FailOnceThenSucceedTask() {
        this.name = "task";
    }

    public FailOnceThenSucceedTask(String name) {
        this.name = name;
    }

    @Override
    public void execute() throws Exception {
        int attempt = callCount.incrementAndGet();
        if (attempt == 1) {
            throw new RuntimeException(name + " failed on attempt " + attempt + " (simulated)");
        }
        System.out.println("    [" + name + "] succeeded on attempt " + attempt);
    }
}