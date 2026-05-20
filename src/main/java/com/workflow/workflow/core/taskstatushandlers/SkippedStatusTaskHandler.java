package com.workflow.workflow.core.taskstatushandlers;

import com.workflow.workflow.core.model.TaskInstance;
import com.workflow.workflow.core.model.Workflow;
import com.workflow.workflow.core.model.WorkflowInstance;
import com.workflow.workflow.core.constants.TaskStatus;
import lombok.RequiredArgsConstructor;

import java.time.Instant;

@RequiredArgsConstructor
public class SkippedStatusTaskHandler implements TaskStatusHandler {

    private final WorkflowInstance workflowInstance;
    private final Workflow workflow;

    @Override
    public void handle(TaskInstance taskInstance) {
        // CAS — only one thread wins the race to skip
        if (!taskInstance.transitionTo(TaskStatus.PENDING, TaskStatus.SKIPPED)) return;

        taskInstance.setEndTime(Instant.now().toString());
        System.out.println("[SKIPPED] Task '" + taskInstance.getTaskId()
                + "' skipped — upstream dependency failed"
                + " [thread: " + Thread.currentThread().getName() + "]");

        // Cascade recursively
        for (TaskInstance dependent : workflowInstance.getDependentInstances(
                taskInstance.getTaskId(), workflow)) {
            handle(dependent);
        }
    }
}