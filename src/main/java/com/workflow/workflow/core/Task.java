package com.workflow.workflow.core;

import com.workflow.workflow.entities.task.TaskStatus;
import lombok.Getter;
import lombok.Setter;

import java.util.*;
import java.util.concurrent.Callable;

@Getter
@Setter
public class Task {
    private final String id;
    private final String taskName;
    private final Set<String> dependencies;   // IDs of tasks that must succeed first
    private final Callable<Void> executionFn;

    private TaskStatus status = TaskStatus.PENDING;

    public Task(String id, String taskName, Set<String> dependencies, Callable<Void> executionFn) {
        this.id = id;
        this.taskName = taskName;
        this.dependencies = Collections.unmodifiableSet(new HashSet<>(dependencies));
        this.executionFn = executionFn;
    }

    public Task(String id, String taskName, Callable<Void> executionFn) {
        this(id, taskName, Collections.emptySet(), executionFn);
    }

    @Override
    public String toString() {
        return "Task{id='" + id + "', deps=" + dependencies + ", status=" + status + "}";
    }
}
