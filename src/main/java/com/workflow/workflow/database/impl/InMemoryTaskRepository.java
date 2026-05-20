package com.workflow.workflow.database.impl;

import com.workflow.workflow.core.model.Task;
import com.workflow.workflow.database.TaskRepository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * InMemoryTaskRepository
 * <p>
 * Stores Task DEFINITIONS only — no runtime state.
 * Thread safety: ConcurrentHashMap, read methods return stable snapshot copies.
 */
public class InMemoryTaskRepository implements TaskRepository {

    private final ConcurrentMap<String, Task> store = new ConcurrentHashMap<>();

    @Override
    public void save(Task task) {
        store.put(task.getId(), task);
    }

    @Override
    public Optional<Task> findById(String taskId) {
        return Optional.ofNullable(store.get(taskId));
    }

    @Override
    public List<Task> findByClassName(String className) {
        return store.values().stream()
                .filter(t -> className.equals(t.getClassName()))
                .collect(Collectors.toList());
    }

    @Override
    public Collection<Task> findAll() {
        return new ArrayList<>(store.values());  // stable snapshot
    }
}