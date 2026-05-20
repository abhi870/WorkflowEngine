package com.workflow.workflow.core.taskstatushandlers;

import com.workflow.workflow.core.model.TaskInstance;


public interface TaskStatusHandler {
    void handle(TaskInstance taskInstance) throws Exception;
}