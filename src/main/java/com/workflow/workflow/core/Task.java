package com.workflow.workflow.core;

import com.workflow.workflow.core.tasks.TaskFunction;
import com.workflow.workflow.entities.task.TaskStatus;
import lombok.Getter;
import lombok.Setter;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

@Getter
@Setter
public class Task {

    private final String id;
    private final String taskName;
    private final Set<String> dependencies;
    private final String className;

    private transient volatile TaskFunction executionFn;
    private volatile String startTime;
    private volatile String endTime;
    private volatile String failureReason;

    private final AtomicReference<TaskStatus> status =
            new AtomicReference<>(TaskStatus.PENDING);


    public Task(String id, String taskName, Set<String> dependencies,
                String className, TaskFunction executionFn) {
        this.id = id;
        this.taskName = taskName;
        this.dependencies = Collections.unmodifiableSet(new HashSet<>(dependencies));
        this.className = className;
        this.executionFn = executionFn;
    }

    /**
     * No dependencies
     */
    public Task(String id, String taskName, String className, TaskFunction executionFn) {
        this(id, taskName, Collections.emptySet(), className, executionFn);
    }


    public TaskStatus getStatus() {
        return status.get();
    }


    public void setStatus(TaskStatus next) {
        status.set(next);
    }

    public boolean transitionTo(TaskStatus expected, TaskStatus next) {
        return status.compareAndSet(expected, next);
    }

    // ── Derived ───────────────────────────────────────────────────────────────

    public long getDurationMs() {
        if (startTime == null || endTime == null) return 0;
        return java.time.Duration.between(
                java.time.Instant.parse(startTime),
                java.time.Instant.parse(endTime)).toMillis();
    }

    @Override
    public String toString() {
        return "Task{"
                + "\n    id            = '" + id + "'"
                + "\n    taskName      = '" + taskName + "'"
                + "\n    className     = '" + className + "'"
                + "\n    dependencies  = " + dependencies
                + "\n    status        = " + status.get()
                + "\n    startTime     = " + (startTime != null ? startTime : "—")
                + "\n    endTime       = " + (endTime != null ? endTime : "—")
                + "\n    durationMs    = " + (startTime != null && endTime != null ? getDurationMs() + "ms" : "—")
                + "\n    failureReason = " + (failureReason != null ? failureReason : "—")
                + "\n  }";
    }
}