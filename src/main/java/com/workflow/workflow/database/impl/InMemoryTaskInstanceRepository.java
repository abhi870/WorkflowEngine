package com.workflow.workflow.database.impl;

import com.workflow.workflow.core.model.TaskInstance;
import com.workflow.workflow.database.TaskInstanceRepository;
import com.workflow.workflow.core.constants.TaskStatus;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * InMemoryTaskInstanceRepository
 * <p>
 * Thread safety:
 * - ConcurrentHashMap for primary store
 * - All read methods return new list copies — stable snapshots
 * - TaskInstance fields are volatile / AtomicReference (on TaskInstance itself)
 */
public class InMemoryTaskInstanceRepository implements TaskInstanceRepository {

    private final ConcurrentMap<String, TaskInstance> store = new ConcurrentHashMap<>();

    @Override
    public void save(TaskInstance taskInstance) {
        store.put(taskInstance.getInstanceId(), taskInstance);
    }

    @Override
    public Optional<TaskInstance> findById(String instanceId) {
        return Optional.ofNullable(store.get(instanceId));
    }

    @Override
    public List<TaskInstance> findByTaskId(String taskId) {
        return store.values().stream()
                .filter(ti -> ti.getTaskId().equals(taskId))
                .collect(Collectors.toList());
    }

    @Override
    public List<TaskInstance> findByWorkflowInstanceId(String workflowInstanceId) {
        return store.values().stream()
                .filter(ti -> ti.getWorkflowInstanceId().equals(workflowInstanceId))
                .collect(Collectors.toList());
    }

    @Override
    public List<TaskInstance> findByStatus(TaskStatus status) {
        return store.values().stream()
                .filter(ti -> ti.getStatus() == status)
                .collect(Collectors.toList());
    }
}
