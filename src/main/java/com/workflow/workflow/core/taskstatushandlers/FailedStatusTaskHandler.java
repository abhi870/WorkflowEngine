package com.workflow.workflow.core.taskstatushandlers;

import com.workflow.workflow.core.Task;
import com.workflow.workflow.core.Workflow;
import com.workflow.workflow.entities.task.TaskStatus;
import lombok.RequiredArgsConstructor;

import java.time.Duration;
import java.time.Instant;

@RequiredArgsConstructor
public class FailedStatusTaskHandler implements TaskStatusHandler {
    private final Workflow workflow;
    private final SkippedStatusTaskHandler skippedHandler;

    @Override
    public TaskStatus handles() {
        return TaskStatus.FAILED;
    }

    @Override
    public void handle(Task task) {
        if (!task.transitionTo(TaskStatus.PENDING, TaskStatus.SKIPPED)) return;
        task.setEndTime(Instant.now().toString());
        System.out.println("[FAILED]  Task '" + task.getId() + "' failed"
                + " after " + getDurationMs(task.getStartTime(), task.getEndTime()) + "ms"
                + " — reason: " + task.getFailureReason());
        for (Task dependent : workflow.getDependents(task.getId())) {
            skippedHandler.handle(dependent);
        }
    }

    private long getDurationMs(String startTime, String endTime) {
        return Duration.between(Instant.parse(startTime), Instant.parse(endTime)).toMillis();
    }
}
