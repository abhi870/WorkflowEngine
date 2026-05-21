package com.workflow.workflow.database;

import com.workflow.workflow.core.constants.TaskStatus;
import com.workflow.workflow.core.model.Task;
import com.workflow.workflow.core.model.TaskInstance;

import java.util.Collection;
import java.util.List;
import java.util.Optional;


public interface WorkflowService {

    void saveAll(String workflowId, Collection<Task> tasks);

    Optional<Task> findTaskById(String taskId);

    List<Task> findByClassName(String className);

    List<Task> findByWorkflow(String workflowId);

    void saveTaskInstance(TaskInstance taskInstance);

    Optional<TaskInstance> findTaskInstanceById(String instanceId);

    List<TaskInstance> findTaskInstancesByWorkflow(String workflowInstanceId);

    TaskStatus getTaskInstanceStatus(String instanceId);
}