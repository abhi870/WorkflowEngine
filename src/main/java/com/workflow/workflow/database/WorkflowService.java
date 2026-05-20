package com.workflow.workflow.database;

import com.workflow.workflow.core.Task;
import com.workflow.workflow.entities.task.TaskStatus;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface WorkflowService {
    void saveAll(String workflowId, Collection<Task> tasks);

    Optional<Task> findById(String taskId);

    TaskStatus getStatus(String taskId);

    List<Task> findByStatus(TaskStatus status);

    List<Task> findByWorkflow(String workflowId);

    List<Task> findByClassName(String className);
}
