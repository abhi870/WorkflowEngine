package com.workflow.workflow.core.taskstatushandlers;

import com.workflow.workflow.core.constants.TaskStatus;
import com.workflow.workflow.core.constants.WorkflowEventType;
import com.workflow.workflow.core.logging.LoggingService;
import com.workflow.workflow.core.logging.WorkflowLog;
import com.workflow.workflow.core.model.TaskInstance;
import com.workflow.workflow.core.model.Workflow;
import com.workflow.workflow.core.model.WorkflowInstance;
import lombok.RequiredArgsConstructor;

import java.time.Instant;

@RequiredArgsConstructor
public class SkippedStatusTaskHandler implements TaskStatusHandler {

    private final WorkflowInstance workflowInstance;
    private final Workflow workflow;
    private final LoggingService loggingService;

    @Override
    public void handle(TaskInstance taskInstance) {
        if (!taskInstance.transitionTo(TaskStatus.PENDING, TaskStatus.SKIPPED)) return;

        taskInstance.setEndTime(Instant.now().toString());
        System.out.println("[SKIPPED] Task '" + taskInstance.getTaskId()
                + "' skipped — upstream dependency failed"
                + " [thread: " + Thread.currentThread().getName() + "]");

        loggingService.logEvent(WorkflowLog.taskEvent(
                workflowInstance.getInstanceId(), workflowInstance.getWorkflowId(),
                taskInstance.getInstanceId(), taskInstance.getTaskId(),
                WorkflowEventType.TASK_SKIPPED,
                "Task skipped — upstream dependency failed"));

        for (TaskInstance dependent : workflowInstance.getDependentInstances(
                taskInstance.getTaskId(), workflow)) {
            handle(dependent);
        }
    }
}