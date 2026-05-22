package com.workflow.workflow.core.model;

import com.workflow.workflow.core.constants.WorkflowStatus;
import com.workflow.workflow.core.util.WorkflowHelper;
import lombok.Getter;

import java.util.*;

@Getter
public class Workflow {
    private final String id;
    private final Set<Task> tasks = new LinkedHashSet<>();
    private volatile WorkflowStatus status = WorkflowStatus.PENDING;


    public Workflow(String id) {
        this.id = id;
    }

    public void addTask(Task task) {
        tasks.add(task);
    }

    public Task getTaskById(String id) {
        return tasks.stream().filter(task -> task.getId() == id).findFirst().orElse(null);
    }

    public void validate() {
        Map<String, Task> idTaskMap = WorkflowHelper.buildIndex(tasks);

        WorkflowHelper.checkNoDuplicateIds(tasks);
        WorkflowHelper.checkDependenciesExist(tasks, idTaskMap);
        WorkflowHelper.checkNoCycles(idTaskMap);
    }


    public Set<Task> getTasks() {
        return Collections.unmodifiableSet(tasks);
    }

    public List<Task> getDependents(String taskId) {
        List<Task> dependents = new ArrayList<>();
        for (Task t : tasks)
            if (t.getDependencies().contains(taskId))
                dependents.add(t);
        return dependents;
    }

}
