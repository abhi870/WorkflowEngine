package com.workflow.workflow.database;

import com.workflow.workflow.core.model.Task;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * TaskRepository
 * <p>
 * Stores Task DEFINITIONS — id, name, deps, className, executionFn.
 * Does NOT store runtime state (status, times) — that lives in TaskInstanceRepository.
 */
public interface TaskRepository {

    void save(Task task);

    Optional<Task> findById(String taskId);

    List<Task> findByClassName(String className);

    Collection<Task> findAll();
}