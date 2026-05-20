package com.workflow.workflow.core.taskstatushandlers;

import com.workflow.workflow.core.model.TaskExecutionLog;
import com.workflow.workflow.core.model.TaskInstance;
import com.workflow.workflow.core.tasks.TaskFunction;
import com.workflow.workflow.core.constants.TaskStatus;
import lombok.RequiredArgsConstructor;

import java.time.Instant;

/**
 * RunningStatusTaskHandler
 * <p>
 * Entry point — called by the engine, not chained from another handler.
 * Does NOT implement TaskStatusHandler because it requires TaskFunction
 * alongside TaskInstance, which the interface doesn't carry.
 * <p>
 * SuccessStatusTaskHandler and FailedStatusTaskHandler implement
 * TaskStatusHandler because they are chained internally.
 */
@RequiredArgsConstructor
public class RunningStatusTaskHandler {

    private final SuccessStatusTaskHandler onSuccess;
    private final FailedStatusTaskHandler onFailure;

    public void handle(TaskInstance taskInstance, TaskFunction executionFn) throws Exception {
        taskInstance.setStatus(TaskStatus.RUNNING);
        String startTime = Instant.now().toString();
        taskInstance.setStartTime(startTime);

        System.out.println("[RUNNING] Task '" + taskInstance.getTaskId() + "'"
                + " [thread: " + Thread.currentThread().getName() + "]");
        try {
            executionFn.execute();
            String endTime = Instant.now().toString();
            taskInstance.appendLog(TaskExecutionLog.success(
                    taskInstance.getInstanceId(),
                    taskInstance.getWorkflowInstanceId(),
                    1, startTime, endTime));
            onSuccess.handle(taskInstance);
        } catch (Exception e) {
            String endTime = Instant.now().toString();
            taskInstance.setFailureReason(e.getMessage());
            taskInstance.appendLog(TaskExecutionLog.failed(
                    taskInstance.getInstanceId(),
                    taskInstance.getWorkflowInstanceId(),
                    1, startTime, endTime, e.getMessage()));
            onFailure.handle(taskInstance);
        }
    }
}