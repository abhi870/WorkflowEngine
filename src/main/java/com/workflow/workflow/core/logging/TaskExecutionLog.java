package com.workflow.workflow.core.logging;

import com.workflow.workflow.core.constants.TaskStatus;
import lombok.Getter;
import lombok.ToString;

import java.time.Instant;


@Getter
@ToString
public class TaskExecutionLog {

    private final String taskInstanceId;
    private final String workflowInstanceId;
    private final int attempt;          // 1-based
    private final TaskStatus status;           // SUCCESS or FAILED only
    private final String startTime;        // attempt execution start
    private final String endTime;          // attempt execution end
    private final String loggedAt;         // when logged to LoggingService
    private final String errorMessage;     // null on SUCCESS

    public TaskExecutionLog(
            String taskInstanceId,
            String workflowInstanceId,
            int attempt,
            TaskStatus status,
            String startTime,
            String endTime,
            String errorMessage
    ) {
        if (status != TaskStatus.SUCCESS && status != TaskStatus.FAILED) {
            throw new IllegalArgumentException(
                    "TaskExecutionLog status must be SUCCESS or FAILED, got: " + status);
        }
        this.taskInstanceId = taskInstanceId;
        this.workflowInstanceId = workflowInstanceId;
        this.attempt = attempt;
        this.status = status;
        this.startTime = startTime;
        this.endTime = endTime;
        this.loggedAt = Instant.now().toString();  // stamped at construction
        this.errorMessage = errorMessage;
    }

    public static TaskExecutionLog success(
            String taskInstanceId, String workflowInstanceId,
            int attempt, String startTime, String endTime) {
        return new TaskExecutionLog(taskInstanceId, workflowInstanceId,
                attempt, TaskStatus.SUCCESS, startTime, endTime, null);
    }

    public static TaskExecutionLog failed(
            String taskInstanceId, String workflowInstanceId,
            int attempt, String startTime, String endTime, String errorMessage) {
        return new TaskExecutionLog(taskInstanceId, workflowInstanceId,
                attempt, TaskStatus.FAILED, startTime, endTime, errorMessage);
    }


    public boolean isSuccess() {
        return status == TaskStatus.SUCCESS;
    }

}