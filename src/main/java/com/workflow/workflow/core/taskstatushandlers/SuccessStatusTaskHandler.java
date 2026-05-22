package com.workflow.workflow.core.taskstatushandlers;

import com.workflow.workflow.core.constants.TaskStatus;
import com.workflow.workflow.core.model.*;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

public class SuccessStatusTaskHandler {


    public void handle(TaskInstance taskInstance, Workflow workflow, WorkflowInstance wfInstance, RunningStatusTaskHandler runningStatusTaskHandler) {
        taskInstance.transitionTo(TaskStatus.RUNNING, TaskStatus.SUCCESS);
        taskInstance.setEndTime(Instant.now().toString());
        System.out.println("[SUCCESS] Task '" + taskInstance.getTaskId() + "'"
                + " completed in " + taskInstance.getDurationMs() + "ms");

        wfInstance.getTaskInstances().stream().forEach(taskInstance1 -> {
            if(taskInstance1.getDependentsStatusMap().containsKey(taskInstance.getTaskId())){
                taskInstance1.getDependentsStatusMap().put(taskInstance.getTaskId(), true);
            }
        });
        List<Task> nextRunTasks = nextRunTasks(taskInstance, workflow.getTasks());

        nextRunTasks.forEach(task -> {
            try {
                if (dependentsDone(taskInstance)) {
                    runningStatusTaskHandler.handle(wfInstance.findByTaskId(task.getId()),
                            task.getExecutionFn(),
                            RetryPolicy.RETRY_ONCE,
                            workflow, wfInstance);
                }
            } catch (Exception e) {
                throw new RuntimeException("Error while running SuccessHandler", e);
            }
        });
    }

    private List<Task> nextRunTasks(TaskInstance taskInstance, Set<Task> tasks) {

        return tasks.stream().filter(task ->
                task.getDependencies().contains(taskInstance.getTaskId())
        ).toList();

    }

    private boolean dependentsDone(TaskInstance taskInstance) {

        ConcurrentMap<String, Boolean> dependentsMap = taskInstance.getDependentsStatusMap();
        return dependentsMap.keySet().stream().allMatch(key -> dependentsMap.get(key) == true);
    }
}