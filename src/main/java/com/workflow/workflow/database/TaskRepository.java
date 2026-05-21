package com.workflow.workflow.database;

import com.workflow.workflow.core.model.Task;

import java.util.List;
import java.util.Optional;


public interface TaskRepository {

    void save(Task task);

    Optional<Task> findById(String taskId);

    List<Task> findByClassName(String className);

}