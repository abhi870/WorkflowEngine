package com.workflow.workflow.core.taskstatushandlers;

import com.workflow.workflow.core.constants.TaskStatus;
import com.workflow.workflow.core.logging.LoggingService;
import com.workflow.workflow.core.logging.TaskExecutionLog;
import com.workflow.workflow.core.model.RetryPolicy;
import com.workflow.workflow.core.model.TaskInstance;
import com.workflow.workflow.core.tasks.TaskFunction;
import lombok.RequiredArgsConstructor;

import java.time.Instant;

@RequiredArgsConstructor
public class RunningStatusTaskHandler {

    private final SuccessStatusTaskHandler onSuccess;
    private final FailedStatusTaskHandler onFailure;
    private final LoggingService loggingService;

    public void handle(TaskInstance taskInstance, TaskFunction executionFn,
                       RetryPolicy retryPolicy) throws Exception {

        taskInstance.transitionTo(TaskStatus.PENDING, TaskStatus.RUNNING);
        taskInstance.setStartTime(Instant.now().toString());

        logTaskRunning(taskInstance, retryPolicy);

        for (int attempt = 1; attempt <= retryPolicy.getMaxAttempts(); attempt++) {
            String startTime = Instant.now().toString();

            try {
                executionFn.execute();

                String endTime = Instant.now().toString();

                loggingService.log(TaskExecutionLog.success(
                        taskInstance.getInstanceId(),
                        taskInstance.getWorkflowInstanceId(),
                        attempt, startTime, endTime));

                if (attempt > 1) {
                    System.out.println("[RETRY-SUCCESS] Task '" + taskInstance.getTaskId()
                            + "' succeeded on attempt " + attempt + "/" + retryPolicy.getMaxAttempts());
                }
                onSuccess.handle(taskInstance);
                return;

            } catch (Exception e) {
                String endTime = Instant.now().toString();
                taskInstance.setFailureReason(e.getMessage());

                loggingService.log(TaskExecutionLog.failed(
                        taskInstance.getInstanceId(),
                        taskInstance.getWorkflowInstanceId(),
                        attempt, startTime, endTime, e.getMessage()));

                boolean hasMoreAttempts = attempt < retryPolicy.getMaxAttempts();

                if (hasMoreAttempts) {
                    logHasMoreAttemptsRetrying(taskInstance, attempt, retryPolicy.getMaxAttempts(), retryPolicy.getBackoffMs());
                    if (retryPolicy.getBackoffMs() > 0) Thread.sleep(retryPolicy.getBackoffMs());
                } else {
                    logAttemptsExhausted(taskInstance, retryPolicy.getMaxAttempts());
                    onFailure.handle(taskInstance);
                }
            }
        }
    }

    private void logTaskRunning(TaskInstance taskInstance, RetryPolicy retryPolicy) {
        System.out.println("[RUNNING] Task '" + taskInstance.getTaskId() + "'"
                + " [thread: " + Thread.currentThread().getName() + "]"
                + " [maxAttempts=" + retryPolicy.getMaxAttempts() + "]");
    }

    private void logAttemptsExhausted(TaskInstance taskInstance, int maxAttempts) {
        System.out.println("[FAILED] Task '" + taskInstance.getTaskId()
                + "' exhausted all " + maxAttempts + " attempt(s).");
    }

    private void logHasMoreAttemptsRetrying(TaskInstance taskInstance, int attempt, int maxAttempts, long backoffMs) {
        System.out.println("[RETRY] Task '" + taskInstance.getTaskId()
                + "' attempt " + attempt + "/" + maxAttempts
                + " failed — retrying in " + backoffMs + "ms...");
    }
}