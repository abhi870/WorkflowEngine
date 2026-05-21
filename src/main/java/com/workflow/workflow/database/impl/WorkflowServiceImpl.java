package com.workflow.workflow.database.impl;

import com.workflow.workflow.core.model.Task;
import com.workflow.workflow.core.model.TaskInstance;
import com.workflow.workflow.database.TaskInstanceRepository;
import com.workflow.workflow.database.TaskRepository;
import com.workflow.workflow.database.WorkflowRepository;
import com.workflow.workflow.database.WorkflowService;
import com.workflow.workflow.core.constants.TaskStatus;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class WorkflowServiceImpl implements WorkflowService {

    private final TaskRepository taskRepository;
    private final TaskInstanceRepository taskInstanceRepository;
    private final WorkflowRepository workflowRepository;

    public WorkflowServiceImpl(TaskRepository taskRepository,
                               TaskInstanceRepository taskInstanceRepository,
                               WorkflowRepository workflowRepository) {
        this.taskRepository = taskRepository;
        this.taskInstanceRepository = taskInstanceRepository;
        this.workflowRepository = workflowRepository;
    }


    @Override
    public void saveAll(String workflowId, Collection<Task> tasks) {
        for (Task task : tasks) {
            taskRepository.save(task);
        }
        workflowRepository.register(workflowId, tasks);
    }

    @Override
    public Optional<Task> findTaskById(String taskId) {
        return taskRepository.findById(taskId);
    }

    @Override
    public List<Task> findByClassName(String className) {
        return taskRepository.findByClassName(className);
    }

    @Override
    public List<Task> findByWorkflow(String workflowId) {
        return workflowRepository.findByWorkflow(workflowId);
    }


    @Override
    public void saveTaskInstance(TaskInstance taskInstance) {
        taskInstanceRepository.save(taskInstance);
    }

    @Override
    public Optional<TaskInstance> findTaskInstanceById(String instanceId) {
        return taskInstanceRepository.findById(instanceId);
    }

    @Override
    public List<TaskInstance> findTaskInstancesByWorkflow(String workflowInstanceId) {
        return taskInstanceRepository.findByWorkflowInstanceId(workflowInstanceId);
    }

    @Override
    public TaskStatus getTaskInstanceStatus(String instanceId) {
        return taskInstanceRepository.findById(instanceId)
                .map(TaskInstance::getStatus)
                .orElseThrow(() -> new IllegalArgumentException(
                        "TaskInstance '" + instanceId + "' not found"));
    }
}