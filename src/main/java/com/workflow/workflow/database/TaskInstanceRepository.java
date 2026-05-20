package com.workflow.workflow.database;

import com.workflow.workflow.core.model.TaskInstance;
import com.workflow.workflow.core.constants.TaskStatus;

import java.util.List;
import java.util.Optional;

/**
 * TaskInstanceRepository
 * <p>
 * Stores TaskInstance runtime state — status, times, logs.
 * Separate from TaskRepository which stores Task definitions.
 */
public interface TaskInstanceRepository {

    void save(TaskInstance taskInstance);

    Optional<TaskInstance> findById(String instanceId);

    /**
     * Find by the task definition ID — returns all instances of that task across runs.
     */
    List<TaskInstance> findByTaskId(String taskId);

    /**
     * Find all instances in a given workflow run.
     */
    List<TaskInstance> findByWorkflowInstanceId(String workflowInstanceId);

    List<TaskInstance> findByStatus(TaskStatus status);
}