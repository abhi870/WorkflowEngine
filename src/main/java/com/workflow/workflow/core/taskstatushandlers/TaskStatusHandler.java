package com.workflow.workflow.core.taskstatushandlers;

import com.workflow.workflow.core.model.TaskInstance;
import com.workflow.workflow.core.model.Workflow;


public interface TaskStatusHandler {
    void handle(TaskInstance taskInstance, Workflow workflow) throws Exception;
}