package com.workflow.workflow.database.impl;

import com.workflow.workflow.core.model.TaskInstance;
import com.workflow.workflow.database.TaskInstanceRepository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;


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
    public List<TaskInstance> findByWorkflowInstanceId(String workflowInstanceId) {
        return store.values().stream()
                .filter(ti -> ti.getWorkflowInstanceId().equals(workflowInstanceId))
                .collect(Collectors.toList());
    }


}
