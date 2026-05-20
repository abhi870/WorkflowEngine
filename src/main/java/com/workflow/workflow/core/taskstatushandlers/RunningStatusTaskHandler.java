package com.workflow.workflow.core.taskstatushandlers;

import com.workflow.workflow.core.Task;
import com.workflow.workflow.entities.task.TaskStatus;
import lombok.RequiredArgsConstructor;

import java.time.Instant;

@RequiredArgsConstructor
public class RunningStatusTaskHandler implements TaskStatusHandler {
    private final SuccessStatusTaskHandler onSuccess;
    private final FailedStatusTaskHandler onFailure;

    @Override
    public TaskStatus handles() {
        return TaskStatus.RUNNING;
    }

    @Override
    public void handle(Task task) {
        task.setStatus(TaskStatus.RUNNING);
        task.setStartTime(Instant.now().toString());
        System.out.println("[RUNNING] Task '" + task.getId() + "' starting"
                + " [thread: " + Thread.currentThread().getName() + "]");
        try {
            task.getExecutionFn().execute();
            onSuccess.handle(task);
        } catch (Exception e) {
            task.setFailureReason(e.getMessage());
            onFailure.handle(task);
        }
    }
}
