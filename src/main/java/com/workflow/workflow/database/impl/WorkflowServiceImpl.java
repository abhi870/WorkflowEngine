package com.workflow.workflow.database.impl;

import com.workflow.workflow.core.Task;
import com.workflow.workflow.database.TaskRepository;
import com.workflow.workflow.database.WorkflowRepository;
import com.workflow.workflow.database.WorkflowService;
import com.workflow.workflow.entities.task.TaskStatus;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class WorkflowServiceImpl implements WorkflowService {

    private final TaskRepository taskRepository;
    private final WorkflowRepository workflowRepository;

    public WorkflowServiceImpl(TaskRepository taskRepository,
                               WorkflowRepository workflowRepository) {
        this.taskRepository = taskRepository;
        this.workflowRepository = workflowRepository;
    }

    // ── Write ─────────────────────────────────────────────────────────────────

    @Override
    public void saveAll(String workflowId, Collection<Task> tasks) {
        for (Task task : tasks) {
            taskRepository.save(task);        // primary store first
        }
        workflowRepository.register(workflowId, tasks);  // index after
    }

    // ── Read — by task ID ─────────────────────────────────────────────────────

    @Override
    public Optional<Task> findById(String taskId) {
        return taskRepository.findById(taskId);
    }

    @Override
    public TaskStatus getStatus(String taskId) {
        return taskRepository.getStatus(taskId);
    }

    @Override
    public List<Task> findByStatus(TaskStatus status) {
        return taskRepository.findByStatus(status);
    }

    // ── Read — by workflow ────────────────────────────────────────────────────

    @Override
    public List<Task> findByWorkflow(String workflowId) {
        return workflowRepository.findByWorkflow(workflowId);
    }

    // ── Read — by class ───────────────────────────────────────────────────────

    @Override
    public List<Task> findByClassName(String className) {
        return taskRepository.findByClassName(className);
    }
}
