package com.workflow.workflow.core.model;

import com.workflow.workflow.core.tasks.TaskFunction;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Task — DEFINITION only.
 * <p>
 * What a task IS: id, name, dependencies, className, executionFn.
 * What a task DOES at runtime: → TaskInstance
 * <p>
 * Immutable after construction.
 */
public class Task {

    private final String id;
    private final String taskName;
    private final Set<String> dependencies;
    private final String className;
    private final TaskFunction executionFn;

    // ── Constructors ──────────────────────────────────────────────────────────

    /**
     * With dependencies
     */
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

    // ── Getters ───────────────────────────────────────────────────────────────

    public String getId() {
        return id;
    }

    public String getTaskName() {
        return taskName;
    }

    public Set<String> getDependencies() {
        return dependencies;
    }

    public String getClassName() {
        return className;
    }

    public TaskFunction getExecutionFn() {
        return executionFn;
    }

    @Override
    public String toString() {
        return "Task{id='" + id + "', taskName='" + taskName
                + "', className='" + className
                + "', deps=" + dependencies + "}";
    }
}