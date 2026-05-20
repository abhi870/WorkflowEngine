package com.workflow.workflow.core;

import lombok.Getter;

import java.util.*;

@Getter
public class Workflow {
    private final String id;
    private final Set<Task> tasks = new LinkedHashSet<>();
    private volatile WorkflowStatus status = WorkflowStatus.PENDING;
    private volatile boolean cancelRequested = false;


    public Workflow(String id) {
        this.id = id;
    }

    public void addTask(Task task) {
        tasks.add(task);
    }

    public void validate() {
        Map<String, Task> idTaskMap = WorkflowHelper.buildIndex(tasks);

        WorkflowHelper.checkNoDuplicateIds(tasks);
        WorkflowHelper.checkDependenciesExist(tasks, idTaskMap);
        WorkflowHelper.checkNoCycles(idTaskMap);
    }

    public void setStatus(WorkflowStatus status) {
        this.status = status;
    }

    public List<Task> getDependents(String taskId) {
        List<Task> dependents = new ArrayList<>();
        for (Task t : tasks)
            if (t.getDependencies().contains(taskId))
                dependents.add(t);
        return dependents;
    }

    public void cancel() {
        this.cancelRequested = true;
        System.out.println("[Workflow] Cancellation requested for '" + id + "'");
    }

}
