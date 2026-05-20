package com.workflow.workflow.database;

import com.workflow.workflow.core.model.Task;
import com.workflow.workflow.core.model.TaskInstance;
import com.workflow.workflow.core.constants.TaskStatus;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * WorkflowService
 * <p>
 * Facade over TaskRepository, TaskInstanceRepository, WorkflowRepository.
 * The engine talks only to this.
 */
public interface WorkflowService {

    // ── Task definitions ──────────────────────────────────────────────────────

    void saveAll(String workflowId, Collection<Task> tasks);

    Optional<Task> findTaskById(String taskId);

    List<Task> findByClassName(String className);

    List<Task> findByWorkflow(String workflowId);

    // ── Task instances ────────────────────────────────────────────────────────

    void saveTaskInstance(TaskInstance taskInstance);

    Optional<TaskInstance> findTaskInstanceById(String instanceId);

    List<TaskInstance> findTaskInstancesByWorkflow(String workflowInstanceId);

    TaskStatus getTaskInstanceStatus(String instanceId);
}