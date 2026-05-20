package com.workflow.workflow.core;

import lombok.Getter;

import java.util.*;

@Getter
public class Workflow {
    private final String id;
    private final Set<Task> tasks = new HashSet<>();

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


}
