package com.workflow.workflow.database.impl;

import com.workflow.workflow.core.Task;
import com.workflow.workflow.database.TaskRepository;
import com.workflow.workflow.database.WorkflowRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

public class InMemoryWorkflowRepository implements WorkflowRepository {

    // ConcurrentHashMap<workflowId, unmodifiableList<taskId>>
    // Values are unmodifiable — written once, never mutated
    private final ConcurrentMap<String, List<String>> workFlowTaskIndex = new ConcurrentHashMap<>();
    private final TaskRepository taskRepository;

    public InMemoryWorkflowRepository(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    /**
     * Register all tasks for a workflow.
     * Uses putIfAbsent — if two threads race on the same workflowId,
     * only the first registration wins.
     */
    public void register(String workflowId, Collection<Task> tasks) {
        List<String> taskIds = tasks.stream()
                .map(Task::getId)
                .collect(Collectors.toUnmodifiableList()); // unmodifiable — safe to share

        workFlowTaskIndex.putIfAbsent(workflowId, taskIds);
    }

    /**
     * Returns all tasks belonging to the given workflow as a stable snapshot.
     */
    public List<Task> findByWorkflow(String workflowId) {
        return workFlowTaskIndex.getOrDefault(workflowId, List.of()).stream()
                .map(taskRepository::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());  // new list — stable snapshot
    }

    public boolean hasWorkflow(String workflowId) {
        return workFlowTaskIndex.containsKey(workflowId);
    }
}