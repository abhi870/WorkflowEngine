package com.workflow.workflow.core.taskstatushandlers;

import com.workflow.workflow.core.Task;
import com.workflow.workflow.core.Workflow;
import com.workflow.workflow.entities.task.TaskStatus;

import java.time.Instant;

public class SkippedStatusTaskHandler implements TaskStatusHandler {
    private final Workflow workflow;

    public SkippedStatusTaskHandler(Workflow workflow) {
        this.workflow = workflow;
    }

    @Override
    public TaskStatus handles() {
        return TaskStatus.SKIPPED;
    }

    @Override
    public void handle(Task task) {
        // Guard: only skip if still pending — already running tasks are not affected
        if (task.getStatus() != TaskStatus.PENDING) return;

        task.setStatus(TaskStatus.SKIPPED);
        task.setEndTime(Instant.now().toString());
        System.out.println("[SKIPPED] Task '" + task.getId()
                + "' skipped — upstream dependency failed");

        // Cascade further down the graph
        for (Task dependent : workflow.getDependents(task.getId())) {
            handle(dependent);
        }
    }
}
