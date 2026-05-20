package com.workflow.workflow.core;

import com.workflow.workflow.core.constants.WorkflowStatus;
import com.workflow.workflow.core.model.*;
import com.workflow.workflow.core.tasks.TaskFunction;
import com.workflow.workflow.core.taskstatushandlers.FailedStatusTaskHandler;
import com.workflow.workflow.core.taskstatushandlers.RunningStatusTaskHandler;
import com.workflow.workflow.core.taskstatushandlers.SkippedStatusTaskHandler;
import com.workflow.workflow.core.taskstatushandlers.SuccessStatusTaskHandler;
import com.workflow.workflow.core.util.WorkflowHelper;
import com.workflow.workflow.database.TaskRegistry;
import com.workflow.workflow.database.WorkflowService;
import com.workflow.workflow.core.constants.TaskStatus;

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
    private final ConcurrentMap<String, WorkflowInstance> activeInstances = new ConcurrentHashMap<>();

    public WorkflowEngine(WorkflowService workflowService, TaskRegistry taskRegistry) {
        this.workflowService = workflowService;
        this.taskRegistry = taskRegistry;
        this.executor = new ThreadPoolExecutor(
                CORE_POOL_SIZE, MAX_POOL_SIZE, KEEP_ALIVE_SECONDS, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(QUEUE_CAPACITY),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    // ── Public API ────────────────────────────────────────────────────────────

    public WorkflowInstance submit(Workflow workflow) throws Exception {
        System.out.println("[Engine] Submitting workflow: " + workflow.getId());
        workflow.validate();
        System.out.println("[Engine] DAG validation passed");

        // Validate all classNames are registered before starting
        for (Task task : workflow.getTasks()) {
            taskRegistry.resolve(task.getClassName());
        }

        // Create one TaskInstance per Task definition
        String wfInstanceId = java.util.UUID.randomUUID().toString();
        List<TaskInstance> taskInstances = new ArrayList<>();
        for (Task task : workflow.getTasks()) {
            TaskInstance ti = new TaskInstance(task.getId(), wfInstanceId);
            taskInstances.add(ti);
            workflowService.saveTaskInstance(ti);  // persist each instance
        }

        // Create WorkflowInstance
        WorkflowInstance wfInstance = new WorkflowInstance(workflow.getId(), taskInstances);
        activeInstances.put(wfInstance.getInstanceId(), wfInstance);
        wfInstance.setStatus(WorkflowStatus.RUNNING);

        // Persist task definitions
        workflowService.saveAll(workflow.getId(), workflow.getTasks());

        // Build handler chain
        SkippedStatusTaskHandler skippedHandler = new SkippedStatusTaskHandler(wfInstance, workflow);
        FailedStatusTaskHandler failedHandler = new FailedStatusTaskHandler(wfInstance, workflow, skippedHandler);
        SuccessStatusTaskHandler successHandler = new SuccessStatusTaskHandler();
        RunningStatusTaskHandler runningHandler = new RunningStatusTaskHandler(successHandler, failedHandler);

        System.out.println("[Engine] Execution plan:");
        List<List<Task>> levels = WorkflowHelper.groupByLevel(workflow.getTasks());
        printExecutionPlan(levels);

        try {
            for (List<Task> level : levels) {
                if (wfInstance.isCancelRequested()) {
                    cancelPendingTaskInstances(level, levels, wfInstance);
                    break;
                }
                runLevel(level, wfInstance, runningHandler);
            }
        } finally {
            activeInstances.remove(wfInstance.getInstanceId());
        }

        // Derive final status
        WorkflowStatus finalStatus;
        if (wfInstance.isCancelRequested()) {
            finalStatus = WorkflowStatus.CANCELLED;
        } else if (wfInstance.anyFailed()) {
            finalStatus = WorkflowStatus.FAILED;
        } else {
            finalStatus = WorkflowStatus.SUCCESS;
        }
        wfInstance.setStatus(finalStatus);
        wfInstance.setEndTime(Instant.now().toString());

        System.out.println("[Engine] Workflow '" + workflow.getId() + "' → " + finalStatus);
        return wfInstance;
    }

    /**
     * Get status of a TaskInstance by its instanceId.
     * Use wfInstance.findByTaskId(taskId).getStatus() for task-definition-based lookup.
     */
    public TaskStatus getTaskInstanceStatus(String instanceId) {
        return workflowService.getTaskInstanceStatus(instanceId);
    }

    public void cancelWorkflow(String workflowId) {
        WorkflowInstance wfInstance = activeInstances.values().stream()
                .filter(w -> w.getWorkflowId().equals(workflowId))
                .findFirst()
                .orElse(null);
        if (wfInstance == null) {
            System.out.println("[Engine] cancelWorkflow('" + workflowId
                    + "') ignored — no active instance found");
            return;
        }
        wfInstance.cancel();
    }

    public void shutdown() {
        executor.shutdown();
        System.out.println("[Engine] Executor shut down");
    }

    public void shutdownNow() {
        List<Runnable> pending = executor.shutdownNow();
        System.out.println("[Engine] Shut down immediately. "
                + pending.size() + " task(s) discarded.");
    }

    // ── Execution ─────────────────────────────────────────────────────────────

    private void runLevel(List<Task> level, WorkflowInstance wfInstance,
                          RunningStatusTaskHandler runningHandler) throws Exception {
        List<Future<?>> futures = new ArrayList<>();

        for (Task task : level) {
            TaskInstance taskInstance = wfInstance.findByTaskId(task.getId());
            if (taskInstance.getStatus() == TaskStatus.SKIPPED) continue;

            TaskFunction fn = task.getExecutionFn();

            futures.add(executor.submit(() -> {
                try {
                    runningHandler.handle(taskInstance, fn);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }));
        }

        for (Future<?> f : futures) f.get();
    }

    private void cancelPendingTaskInstances(List<Task> currentLevel,
                                            List<List<Task>> allLevels,
                                            WorkflowInstance wfInstance) {
        int currentIndex = allLevels.indexOf(currentLevel);
        for (int i = currentIndex; i < allLevels.size(); i++) {
            for (Task task : allLevels.get(i)) {
                TaskInstance ti = wfInstance.findByTaskId(task.getId());
                if (ti.getStatus() == TaskStatus.PENDING) {
                    ti.setStatus(TaskStatus.CANCELLED);
                    ti.setEndTime(Instant.now().toString());
                    System.out.println("[CANCELLED] Task '" + task.getId() + "'");
                }
            }
        }
    }

    private void printExecutionPlan(List<List<Task>> levels) {
        for (int i = 0; i < levels.size(); i++) {
            List<String> ids = levels.get(i).stream().map(Task::getId).toList();
            System.out.println("         Level " + i + ": " + ids
                    + (ids.size() > 1 ? " (parallel)" : ""));
        }
    }
}