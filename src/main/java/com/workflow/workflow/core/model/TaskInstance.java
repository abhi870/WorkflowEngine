package com.workflow.workflow.core.model;


import com.workflow.workflow.core.constants.TaskStatus;
import com.workflow.workflow.core.logging.TaskExecutionLog;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;


@Setter
@Getter
@ToString
public class TaskInstance {

    private final String instanceId;
    private final String taskId;               // FK → Task definition
    private final String workflowInstanceId;   // FK → WorkflowInstance

    private final AtomicReference<TaskStatus> status =
            new AtomicReference<>(TaskStatus.PENDING);

    private volatile String startTime;
    private volatile String endTime;
    private volatile String failureReason;

    private final List<TaskExecutionLog> logs = Collections.synchronizedList(new ArrayList<>());

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


    public void setStatus(TaskStatus next) {
        status.set(next);
    }


    public void appendLog(TaskExecutionLog log) {
        logs.add(log);
    }


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


}