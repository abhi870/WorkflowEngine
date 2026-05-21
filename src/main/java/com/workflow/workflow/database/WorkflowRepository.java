package com.workflow.workflow.database;

import com.workflow.workflow.core.model.Task;

import java.util.Collection;
import java.util.List;

public interface WorkflowRepository {

    void register(String workflowId, Collection<Task> tasks);

    List<Task> findByWorkflow(String workflowId);

}