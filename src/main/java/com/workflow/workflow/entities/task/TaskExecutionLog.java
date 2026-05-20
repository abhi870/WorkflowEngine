package com.workflow.workflow.entities.task;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;


@Getter
public class TaskExecutionLog {
    private final String taskInstanceId;
    private final String workflowInstanceId;
    private final int attempt;
    private final TaskStatus status;
    private final Instant startTime;
    private final Instant endTime;
    private final String errorMessage;

    public TaskExecutionLog(
            String taskInstanceId,
            String workflowInstanceId,
            int attempt,
            TaskStatus status,
            Instant startTime,
            Instant endTime,
            String errorMessage
    ) {
        if (attempt < 1) {
            throw new IllegalArgumentException("attempt must be >= 1, got: " + attempt);
        }
        if (status != TaskStatus.SUCCESS && status != TaskStatus.FAILED) {
            throw new IllegalArgumentException(
                    "TaskExecutionLog status must be SUCCESS or FAILED, got: " + status);
        }
        if (endTime.isBefore(startTime)) {
            throw new IllegalArgumentException("endTime cannot be before startTime");
        }

        this.taskInstanceId = taskInstanceId;
        this.workflowInstanceId = workflowInstanceId;
        this.attempt = attempt;
        this.status = status;
        this.startTime = startTime;
        this.endTime = endTime;
        this.errorMessage = errorMessage;
    }
}
