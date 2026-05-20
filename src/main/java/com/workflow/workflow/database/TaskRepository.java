package com.workflow.workflow.database;

import com.workflow.workflow.core.Task;
import com.workflow.workflow.entities.task.TaskStatus;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TaskRepository {
    public void save(Task task);


    public Optional<Task> findById(String taskId);

    public TaskStatus getStatus(String taskId);

    public List<Task> findByStatus(TaskStatus status);

    public List<Task> findByClassName(String className);

    public Collection<Task> findAll();
}
