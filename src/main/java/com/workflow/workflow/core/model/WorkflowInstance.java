package com.workflow.workflow.core.model;

import com.workflow.workflow.core.constants.TaskStatus;
import com.workflow.workflow.core.constants.WorkflowStatus;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;


@Getter
@Setter
@ToString
public class WorkflowInstance {

    private final String instanceId;
    private final String workflowId;        // FK → Workflow definition
    private final String startTime;
    private final List<TaskInstance> taskInstances;

    private volatile WorkflowStatus status = WorkflowStatus.PENDING;
    private volatile boolean cancelRequested = false;
    private volatile String endTime;

    public WorkflowInstance(String workflowId, List<TaskInstance> taskInstances) {
        this.instanceId = UUID.randomUUID().toString();
        this.workflowId = workflowId;
        this.taskInstances = Collections.unmodifiableList(new ArrayList<>(taskInstances));
        this.startTime = Instant.now().toString();
    }


    public List<TaskInstance> getTaskInstances() {
        return Collections.unmodifiableList(taskInstances);
    }


    public void cancel() {
        this.cancelRequested = true;
        System.out.println("[WorkflowInstance] Cancellation requested for '" + instanceId + "'");
    }


    public TaskInstance findByTaskId(String taskId) {
        return taskInstances.stream()
                .filter(ti -> ti.getTaskId().equals(taskId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No TaskInstance for taskId: " + taskId));
    }

    public List<TaskInstance> getDependentInstances(String taskId, Workflow workflow) {
        List<String> dependentTaskIds = workflow.getDependents(taskId)
                .stream().map(Task::getId).toList();
        return taskInstances.stream()
                .filter(ti -> dependentTaskIds.contains(ti.getTaskId()))
                .toList();
    }

    public boolean anyFailed() {
        return taskInstances.stream()
                .anyMatch(ti -> ti.getStatus() == TaskStatus.FAILED);
    }


}