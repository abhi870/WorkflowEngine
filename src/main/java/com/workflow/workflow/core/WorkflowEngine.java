package com.workflow.workflow.core;

import com.workflow.workflow.core.taskstatushandlers.*;
import com.workflow.workflow.database.WorkflowService;
import com.workflow.workflow.entities.task.TaskStatus;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class WorkflowEngine {

    private static final int CORE_POOL_SIZE = 4;
    private static final int MAX_POOL_SIZE = 16;
    private static final long KEEP_ALIVE_SECONDS = 60L;
    private static final int QUEUE_CAPACITY = 100;

    private final ThreadPoolExecutor executor;
    private final WorkflowService workflowService;
    private final TaskRegistry taskRegistry;
    private final ConcurrentMap<String, Workflow> activeWorkflows = new ConcurrentHashMap<>();

    public WorkflowEngine(WorkflowService workflowService, TaskRegistry taskRegistry) {
        this.workflowService = workflowService;
        this.taskRegistry = taskRegistry;
        this.executor = new ThreadPoolExecutor(
                CORE_POOL_SIZE,
                MAX_POOL_SIZE,
                KEEP_ALIVE_SECONDS, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(QUEUE_CAPACITY),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    // ── Public API ────────────────────────────────────────────────────────────

    public void submit(Workflow workflow) throws Exception {
        activeWorkflows.put(workflow.getId(), workflow);
        workflow.setStatus(WorkflowStatus.RUNNING);

        System.out.println("[Engine] Submitting workflow: " + workflow.getId());
        workflow.validate();
        System.out.println("[Engine] DAG validation passed");

        // Persist to repository
        workflowService.saveAll(workflow.getId(), workflow.getTasks());

        // Resolve executionFn from className via registry.
        // Skip "inline" tasks — executionFn already set via lambda constructor.
        for (Task task : workflow.getTasks()) {
            if (!"inline".equals(task.getClassName())) {
                task.setExecutionFn(() -> taskRegistry.resolve(task.getClassName()).call());
            }
        }

        TaskStatusHandler entryHandler = buildHandlerChain(workflow);

        List<List<Task>> levels = WorkflowHelper.groupByLevel(workflow.getTasks());
        System.out.println("[Engine] Execution plan:");
        printEngineExecutionPlan(levels);

        try {
            for (List<Task> level : levels) {
                if (workflow.isCancelRequested()) {
                    cancelPendingTasks(level, levels, workflow);
                    break;
                }
                runLevel(level, entryHandler);
            }
        } finally {
            activeWorkflows.remove(workflow.getId());
        }

        WorkflowStatus finalStatus;
        if (workflow.isCancelRequested()) {
            finalStatus = WorkflowStatus.CANCELLED;
        } else if (workflow.getTasks().stream().anyMatch(t -> t.getStatus() == TaskStatus.FAILED)) {
            finalStatus = WorkflowStatus.FAILED;
        } else {
            finalStatus = WorkflowStatus.SUCCESS;
        }
        workflow.setStatus(finalStatus);
        System.out.println("[Engine] Workflow '" + workflow.getId() + "' → " + finalStatus);
    }

    /**
     * Get current status of any task — delegates to WorkflowService.
     */
    public TaskStatus getTaskStatus(String taskId) {
        return workflowService.getStatus(taskId);
    }

    public void cancelWorkflow(String workflowId) {
        Workflow workflow = activeWorkflows.get(workflowId);
        if (workflow == null) {
            // Workflow already completed or never existed — cancellation is a no-op
            System.out.println("[Engine] cancelWorkflow('" + workflowId
                    + "') ignored — workflow is not active (already completed?)");
            return;
        }
        workflow.cancel();
    }

    public void shutdown() {
        executor.shutdown();
        System.out.println("[Engine] Executor shut down");
    }

    public void shutdownNow() {
        List<Runnable> pending = executor.shutdownNow();
        System.out.println("[Engine] Executor shut down immediately. "
                + pending.size() + " queued task(s) discarded.");
    }

    // ── Execution ─────────────────────────────────────────────────────────────

    private void runLevel(List<Task> level, TaskStatusHandler entryHandler) throws Exception {
        List<Future<?>> futures = new ArrayList<>();
        for (Task task : level) {
            if (task.getStatus() == TaskStatus.SKIPPED) continue;
            futures.add(executor.submit(() -> {
                try {
                    entryHandler.handle(task);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }));
        }
        for (Future<?> f : futures) f.get();
    }

    private void cancelPendingTasks(List<Task> currentLevel,
                                    List<List<Task>> allLevels,
                                    Workflow workflow) {
        int currentIndex = allLevels.indexOf(currentLevel);
        for (int i = currentIndex; i < allLevels.size(); i++) {
            for (Task task : allLevels.get(i)) {
                if (task.getStatus() == TaskStatus.PENDING) {
                    task.setStatus(TaskStatus.CANCELLED);
                    task.setEndTime(Instant.now().toString());
                    System.out.println("[CANCELLED] Task '" + task.getId()
                            + "' cancelled — workflow cancellation requested");
                }
            }
        }
    }

    private TaskStatusHandler buildHandlerChain(Workflow workflow) {
        SkippedStatusTaskHandler skippedHandler = new SkippedStatusTaskHandler(workflow);
        FailedStatusTaskHandler failedHandler = new FailedStatusTaskHandler(workflow, skippedHandler);
        SuccessStatusTaskHandler successHandler = new SuccessStatusTaskHandler();
        return new RunningStatusTaskHandler(successHandler, failedHandler);
    }

    private void printEngineExecutionPlan(List<List<Task>> levels) {
        for (int i = 0; i < levels.size(); i++) {
            List<String> ids = levels.get(i).stream().map(Task::getId).toList();
            System.out.println("         Level " + i + ": " + ids
                    + (ids.size() > 1 ? " (parallel)" : ""));
        }
    }
}