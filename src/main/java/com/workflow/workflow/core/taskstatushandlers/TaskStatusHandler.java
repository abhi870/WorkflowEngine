package com.workflow.workflow.core.taskstatushandlers;

import com.workflow.workflow.core.Task;
import com.workflow.workflow.entities.task.TaskStatus;

public interface TaskStatusHandler {
    public TaskStatus handles();

    public void handle(Task task);

}
