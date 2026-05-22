package com.workflow.workflow.core.taskstatushandlers;

import com.workflow.workflow.core.constants.TaskStatus;
import com.workflow.workflow.core.model.TaskInstance;
import com.workflow.workflow.core.model.Workflow;
import com.workflow.workflow.core.model.WorkflowInstance;
import lombok.RequiredArgsConstructor;

import java.time.Instant;

@RequiredArgsConstructor
public class FailedStatusTaskHandler {

    private final WorkflowInstance workflowInstance;
    private final Workflow workflow;
    private final SkippedStatusTaskHandler skippedHandler;


    public void handle(TaskInstance taskInstance) {
        taskInstance.transitionTo(TaskStatus.RUNNING, TaskStatus.FAILED);
        taskInstance.setEndTime(Instant.now().toString());
        System.out.println("[FAILED]  Task '" + taskInstance.getTaskId() + "'"
                + " failed after " + taskInstance.getDurationMs() + "ms"
                + " — reason: " + taskInstance.getFailureReason());

        for (TaskInstance dependent : workflowInstance.getDependentInstances(
                taskInstance.getTaskId(), workflow)) {
            skippedHandler.handle(dependent);
        }
    }
}