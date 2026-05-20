package com.workflow.workflow.core.taskstatushandlers;

import com.workflow.workflow.core.Task;
import com.workflow.workflow.entities.task.TaskStatus;

import java.time.Duration;
import java.time.Instant;

public class SuccessStatusTaskHandler implements TaskStatusHandler {
    @Override
    public TaskStatus handles() {
        return TaskStatus.SUCCESS;
    }

    @Override
    public void handle(Task task) {
        task.setStatus(TaskStatus.SUCCESS);
        task.setEndTime(Instant.now().toString());
        System.out.println("[SUCCESS] Task '" + task.getId() + "' completed"
                + " in " + getDurationMs(task.getStartTime(), task.getEndTime()) + "ms");
    }

    private long getDurationMs(String startTime, String endTime) {
        return Duration.between(Instant.parse(startTime), Instant.parse(endTime)).toMillis();
    }
}
