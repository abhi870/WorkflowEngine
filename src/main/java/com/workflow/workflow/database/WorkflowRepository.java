package com.workflow.workflow.database;

import com.workflow.workflow.core.Task;

import java.util.Collection;
import java.util.List;

public interface WorkflowRepository {
    public void register(String workflowId, Collection<Task> tasks);

    public List<Task> findByWorkflow(String workflowId);

    public boolean hasWorkflow(String workflowId);
}
