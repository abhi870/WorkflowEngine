package com.workflow.workflow.core;

import com.workflow.workflow.entities.task.TaskStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class WorkflowEngine {
    private final ExecutorService executor;

    public WorkflowEngine() {
        this.executor = Executors.newCachedThreadPool();
    }

    public void submit(Workflow workflow) throws Exception {
        System.out.println("[Engine] Submitting workflow: " + workflow.getId());

        // 1. Validate
        try {
            workflow.validate();
            System.out.println("[Engine] DAG validation passed");
        } catch (IllegalStateException e) {
            System.out.println("[Engine] DAG validation FAILED: " + e.getMessage());
            throw e;
        }

        // 2. Group into levels
        List<List<Task>> levels = WorkflowHelper.groupByLevel(workflow.getTasks());
        System.out.println("[Engine] Execution plan:");
        for (int i = 0; i < levels.size(); i++) {
            List<String> ids = levels.get(i).stream().map(Task::getId).toList();
            System.out.println("         Level " + i + ": " + ids
                    + (ids.size() > 1 ? " (parallel)" : ""));
        }

        for (List<Task> level : levels) {
            runLevel(level);
        }

        System.out.println("[Engine] Workflow '" + workflow.getId() + "' completed successfully");
        executor.shutdown();
    }


    private void runLevel(List<Task> level) throws Exception {
        List<Future<?>> futures = new ArrayList<>();

        for (Task task : level) {
            task.setStatus(TaskStatus.RUNNING);
            System.out.println("[Engine] Dispatching task: " + task.getId()
                    + " [thread: " + Thread.currentThread().getName() + "]");

            Future<?> future = executor.submit(() -> {
                System.out.println("[Engine] Running task: " + task.getId()
                        + " [thread: " + Thread.currentThread().getName() + "]");
                try {
                    task.getExecutionFn().call();
                    task.setStatus(TaskStatus.SUCCESS);
                    System.out.println("[Engine] Task '" + task.getId() + "' -> SUCCESS");
                } catch (Exception e) {
                    task.setStatus(TaskStatus.FAILED);
                    System.out.println("[Engine] Task '" + task.getId() + "' -> FAILED: " + e.getMessage());
                    throw new RuntimeException(e);
                }
            });

            futures.add(future);
        }

        for (Future<?> f : futures) {
            f.get();
        }
    }
}
