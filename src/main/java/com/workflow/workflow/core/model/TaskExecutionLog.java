package com.workflow.workflow.core.model;

import com.workflow.workflow.core.constants.TaskStatus;

/**
 * TaskExecutionLog
 * <p>
 * One record per ATTEMPT, not per task instance.
 * A retried task produces multiple logs — one per attempt.
 * Immutable — written once when an attempt completes.
 */
public class TaskExecutionLog {

    private final String taskInstanceId;
    private final String workflowInstanceId;
    private final int attempt;          // 1-based
    private final TaskStatus status;           // SUCCESS or FAILED only
    private final String startTime;
    private final String endTime;
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
        this.errorMessage = errorMessage;
    }

    // ── Factory methods ───────────────────────────────────────────────────────

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

    // ── Getters ───────────────────────────────────────────────────────────────

    public String getTaskInstanceId() {
        return taskInstanceId;
    }

    public String getWorkflowInstanceId() {
        return workflowInstanceId;
    }

    public int getAttempt() {
        return attempt;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public String getStartTime() {
        return startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public boolean isSuccess() {
        return status == TaskStatus.SUCCESS;
    }

    @Override
    public String toString() {
        String err = errorMessage != null ? " | error='" + errorMessage + "'" : "";
        return "TaskExecutionLog{attempt=" + attempt
                + ", status=" + status
                + err + "}";
    }
}