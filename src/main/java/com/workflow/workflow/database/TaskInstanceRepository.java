package com.workflow.workflow.database;

import com.workflow.workflow.core.model.TaskInstance;

import java.util.List;
import java.util.Optional;


public interface TaskInstanceRepository {

    void save(TaskInstance taskInstance);

    Optional<TaskInstance> findById(String instanceId);

    List<TaskInstance> findByWorkflowInstanceId(String workflowInstanceId);

}