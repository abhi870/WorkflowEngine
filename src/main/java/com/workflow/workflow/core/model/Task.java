package com.workflow.workflow.core.model;

import com.workflow.workflow.core.tasks.TaskFunction;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.*;

@Builder
@Getter
@ToString
public class Task {

    private final String id;
    private final String taskName;
    private final Set<String> dependencies;
    private final String className;
    private final TaskFunction executionFn;
    private final RetryPolicy retryPolicy;


    public Task(String id, String taskName, Set<String> dependencies,
                String className, TaskFunction executionFn, RetryPolicy retryPolicy) {
        this.id = id;
        this.taskName = taskName;
        this.dependencies = Collections.unmodifiableSet(new HashSet<>(dependencies));
        this.className = className;
        this.executionFn = executionFn;
        this.retryPolicy = retryPolicy;
    }

    public Task(String id, String taskName, Set<String> dependencies,
                String className, TaskFunction executionFn) {
        this(id, taskName, dependencies, className, executionFn, RetryPolicy.NO_RETRY);
    }

    public Task(String id, String taskName, String className,
                TaskFunction executionFn, RetryPolicy retryPolicy) {
        this(id, taskName, Collections.emptySet(), className, executionFn, retryPolicy);
    }

    public Task(String id, String taskName, String className, TaskFunction executionFn) {
        this(id, taskName, Collections.emptySet(), className, executionFn, RetryPolicy.NO_RETRY);
    }



}