package com.workflow.workflow.database.impl;

import com.workflow.workflow.core.Task;
import com.workflow.workflow.database.TaskRepository;
import com.workflow.workflow.entities.task.TaskStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

public class InMemoryTaskRepository implements TaskRepository {

    private final ConcurrentMap<String, Task> store = new ConcurrentHashMap<>();

    // ── Write ─────────────────────────────────────────────────────────────────

    public void save(Task task) {
        store.put(task.getId(), task);
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    public Optional<Task> findById(String taskId) {
        return Optional.ofNullable(store.get(taskId));
    }

    public TaskStatus getStatus(String taskId) {
        return findById(taskId)
                .map(Task::getStatus)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Task '" + taskId + "' not found"));
    }

    public List<Task> findByStatus(TaskStatus status) {
        // Collect to new list — caller gets a stable snapshot
        return store.values().stream()
                .filter(t -> t.getStatus() == status)
                .collect(Collectors.toList());
    }

    public List<Task> findByClassName(String className) {
        return store.values().stream()
                .filter(t -> className.equals(t.getClassName()))
                .collect(Collectors.toList());
    }

    /**
     * Returns a stable snapshot copy — not a live view.
     * Callers iterating the result won't see concurrent modifications.
     */
    public List<Task> findAll() {
        return new ArrayList<>(store.values());
    }
}
