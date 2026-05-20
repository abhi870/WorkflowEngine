package com.workflow.workflow.core.model;


import com.workflow.workflow.core.constants.TaskStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * TaskInstance
 * <p>
 * A specific EXECUTION of a Task definition.
 * One Task can be executed many times — each produces a TaskInstance.
 * <p>
 * Owns all runtime state:
 * status, startTime, endTime, failureReason, logs
 * <p>
 * Task (definition) owns:
 * id, taskName, dependencies, className, executionFn
 */
@Setter
@Getter
public class TaskInstance {

    private final String instanceId;
    private final String taskId;               // FK → Task definition
    private final String workflowInstanceId;   // FK → WorkflowInstance

    // Runtime state — thread-safe
    private final AtomicReference<TaskStatus> status =
            new AtomicReference<>(TaskStatus.PENDING);

    private volatile String startTime;
    private volatile String endTime;
    private volatile String failureReason;

    // One log per attempt — appended by handlers
    private final List<TaskExecutionLog> logs = new ArrayList<>();

    public TaskInstance(String taskId, String workflowInstanceId) {
        this.instanceId = UUID.randomUUID().toString();
        this.taskId = taskId;
        this.workflowInstanceId = workflowInstanceId;
    }


    public TaskStatus getStatus() {
        return status.get();
    }


    public List<TaskExecutionLog> getLogs() {
        return Collections.unmodifiableList(logs);
    }

    // ── Setters — called by handlers ─────────────────────────────────────────

    public void setStatus(TaskStatus next) {
        status.set(next);
    }


    public void appendLog(TaskExecutionLog log) {
        logs.add(log);
    }

    /**
     * Atomic CAS — used by SkippedStatusTaskHandler to prevent double-skip.
     */
    public boolean transitionTo(TaskStatus expected, TaskStatus next) {
        return status.compareAndSet(expected, next);
    }

    public boolean isTerminal() {
        TaskStatus s = status.get();
        return s == TaskStatus.SUCCESS || s == TaskStatus.FAILED
                || s == TaskStatus.SKIPPED || s == TaskStatus.CANCELLED;
    }

    public long getDurationMs() {
        if (startTime == null || endTime == null) return 0;
        return java.time.Duration.between(
                Instant.parse(startTime), Instant.parse(endTime)).toMillis();
    }

    @Override
    public String toString() {
        return "TaskInstance{"
                + "\n    instanceId   = '" + instanceId + "'"
                + "\n    taskId       = '" + taskId + "'"
                + "\n    status       = " + status.get()
                + "\n    startTime    = " + (startTime != null ? startTime : "—")
                + "\n    endTime      = " + (endTime != null ? endTime : "—")
                + "\n    durationMs   = " + (startTime != null && endTime != null ? getDurationMs() + "ms" : "—")
                + "\n    failureReason= " + (failureReason != null ? failureReason : "—")
                + "\n    logs         = " + logs.size() + " attempt(s)"
                + "\n  }";
    }
}