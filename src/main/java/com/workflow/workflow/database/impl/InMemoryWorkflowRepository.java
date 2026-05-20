package com.workflow.workflow.database.impl;

import com.workflow.workflow.core.model.Task;
import com.workflow.workflow.database.TaskRepository;
import com.workflow.workflow.database.WorkflowRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

public class InMemoryWorkflowRepository implements WorkflowRepository {

    private final ConcurrentMap<String, List<String>> workFlowTaskIndex = new ConcurrentHashMap<>();
    private final TaskRepository taskRepository;

    public InMemoryWorkflowRepository(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @Override
    public void register(String workflowId, Collection<Task> tasks) {
        List<String> taskIds = tasks.stream()
                .map(Task::getId)
                .collect(Collectors.toUnmodifiableList());
        workFlowTaskIndex.putIfAbsent(workflowId, taskIds);
    }

    @Override
    public List<Task> findByWorkflow(String workflowId) {
        return workFlowTaskIndex.getOrDefault(workflowId, List.of()).stream()
                .map(taskRepository::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    @Override
    public boolean hasWorkflow(String workflowId) {
        return workFlowTaskIndex.containsKey(workflowId);
    }
}