package com.workflow.workflow.core.taskstatushandlers;

import com.workflow.workflow.core.model.TaskInstance;
import com.workflow.workflow.core.constants.TaskStatus;

import java.time.Instant;

public class SuccessStatusTaskHandler implements TaskStatusHandler {

    @Override
    public void handle(TaskInstance taskInstance) {
        taskInstance.setStatus(TaskStatus.SUCCESS);
        taskInstance.setEndTime(Instant.now().toString());
        System.out.println("[SUCCESS] Task '" + taskInstance.getTaskId() + "'"
                + " completed in " + taskInstance.getDurationMs() + "ms");
    }
}