package com.workflow.workflow.core;

import com.workflow.workflow.core.constants.TaskStatus;
import com.workflow.workflow.core.constants.WorkflowEventType;
import com.workflow.workflow.core.constants.WorkflowStatus;
import com.workflow.workflow.core.logging.LoggingService;
import com.workflow.workflow.core.logging.WorkflowLog;
import com.workflow.workflow.core.model.*;
import com.workflow.workflow.core.tasks.TaskFunction;
import com.workflow.workflow.core.taskstatushandlers.FailedStatusTaskHandler;
import com.workflow.workflow.core.taskstatushandlers.RunningStatusTaskHandler;
import com.workflow.workflow.core.taskstatushandlers.SkippedStatusTaskHandler;
import com.workflow.workflow.core.taskstatushandlers.SuccessStatusTaskHandler;
import com.workflow.workflow.core.util.WorkflowHelper;
import com.workflow.workflow.database.TaskRegistry;
import com.workflow.workflow.database.WorkflowService;

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
    private final ConcurrentMap<String, WorkflowInstance> activeWorkflowInstances = new ConcurrentHashMap<>();
    private final LoggingService loggingService;

    public WorkflowEngine(WorkflowService workflowService, TaskRegistry taskRegistry,
                          LoggingService loggingService) {
        this.workflowService = workflowService;
        this.taskRegistry = taskRegistry;
        this.loggingService = loggingService;
        this.executor = new ThreadPoolExecutor(
                CORE_POOL_SIZE, MAX_POOL_SIZE, KEEP_ALIVE_SECONDS, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(QUEUE_CAPACITY),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }


    public WorkflowInstance submit(Workflow workflow) throws Exception {
        System.out.println("[Engine] Submitting workflow: " + workflow.getId());
        workflow.validate();
        System.out.println("[Engine] DAG validation passed");


        for (Task task : workflow.getTasks()) {
            taskRegistry.resolve(task.getClassName());
        }

        // Create one TaskInstance per Task definition
        String wfInstanceId = java.util.UUID.randomUUID().toString();
        List<TaskInstance> taskInstances = new ArrayList<>();
        for (Task task : workflow.getTasks()) {
            TaskInstance taskInstance = new TaskInstance(task.getId(), wfInstanceId);
            taskInstances.add(taskInstance);
            workflowService.saveTaskInstance(taskInstance);  // persist each instance
        }

        WorkflowInstance wfInstance = new WorkflowInstance(workflow.getId(), taskInstances);
        activeWorkflowInstances.putIfAbsent(wfInstance.getWorkflowId(), wfInstance);
        wfInstance.setStatus(WorkflowStatus.RUNNING);

        loggingService.logEvent(WorkflowLog.workflowEvent(
                wfInstance.getInstanceId(), workflow.getId(),
                WorkflowEventType.WORKFLOW_SUBMITTED,
                "Workflow submitted with " + workflow.getTasks().size() + " tasks"));
        loggingService.logEvent(WorkflowLog.workflowEvent(
                wfInstance.getInstanceId(), workflow.getId(),
                WorkflowEventType.WORKFLOW_VALIDATED,
                "DAG validation passed"));

        workflowService.saveAll(workflow.getId(), workflow.getTasks());

        SkippedStatusTaskHandler skippedHandler = new SkippedStatusTaskHandler(wfInstance, workflow, loggingService);
        FailedStatusTaskHandler failedHandler = new FailedStatusTaskHandler(wfInstance, workflow, skippedHandler);
        SuccessStatusTaskHandler successHandler = new SuccessStatusTaskHandler();
        RunningStatusTaskHandler runningHandler = new RunningStatusTaskHandler(successHandler, failedHandler, loggingService);

        System.out.println("[Engine] Execution plan:");
        List<List<Task>> levels = WorkflowHelper.groupByLevel(workflow.getTasks());
        printExecutionPlan(levels);

        try {
            for (int levelIndex = 0; levelIndex < levels.size(); levelIndex++) {

                List<Task> level = levels.get(levelIndex);
                if (wfInstance.isCancelRequested()) {
                    cancelPendingTaskInstances(level, levels, wfInstance);
                    break;
                }


                List<String> levelTaskIds = level.stream().map(Task::getId).toList();
                loggingService.logEvent(WorkflowLog.workflowEvent(
                        wfInstance.getInstanceId(), workflow.getId(),
                        WorkflowEventType.LEVEL_STARTED,
                        "Level " + levelIndex + " started: " + levelTaskIds));

                runLevel(level, wfInstance, runningHandler);
            }
        } finally {
            activeWorkflowInstances.remove(wfInstance.getWorkflowId());
        }

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

        loggingService.logEvent(WorkflowLog.workflowEvent(
                wfInstance.getInstanceId(), workflow.getId(),
                WorkflowEventType.WORKFLOW_COMPLETED,
                "Workflow completed with status: " + finalStatus));

        return wfInstance;
    }


    public TaskStatus getTaskInstanceStatus(String instanceId) {
        return workflowService.getTaskInstanceStatus(instanceId);
    }

    public void cancelWorkflow(String workflowId) {
        WorkflowInstance wfInstance = activeWorkflowInstances.values().stream()
                .filter(w -> w.getWorkflowId().equals(workflowId))
                .findFirst()
                .orElse(null);
        if (wfInstance == null) {
            System.out.println("[Engine] cancelWorkflow('" + workflowId
                    + "') ignored — no active instance found");
            return;
        }
        loggingService.logEvent(WorkflowLog.workflowEvent(
                wfInstance.getInstanceId(), workflowId,
                WorkflowEventType.WORKFLOW_CANCEL_REQUESTED,
                "Cancellation requested for workflow: " + workflowId));
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


    private void runLevel(List<Task> level, WorkflowInstance wfInstance,
                          RunningStatusTaskHandler runningHandler) throws Exception {
        List<Future<?>> futures = new ArrayList<>();

        for (Task task : level) {
            TaskInstance taskInstance = wfInstance.findByTaskId(task.getId());
            if (taskInstance.getStatus() == TaskStatus.SKIPPED) continue;

            TaskFunction fn = task.getExecutionFn();
            RetryPolicy retryPolicy = task.getRetryPolicy();
            futures.add(executor.submit(() -> {
                try {
                    runningHandler.handle(taskInstance, fn, retryPolicy);
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
                TaskInstance taskInstance = wfInstance.findByTaskId(task.getId());
                if (taskInstance.getStatus() == TaskStatus.PENDING) {
                    taskInstance.transitionTo(TaskStatus.PENDING, TaskStatus.CANCELLED);
                    taskInstance.setEndTime(Instant.now().toString());
                    System.out.println("[CANCELLED] Task '" + task.getId() + "'");
                    loggingService.logEvent(WorkflowLog.taskEvent(
                            wfInstance.getInstanceId(), wfInstance.getWorkflowId(),
                            taskInstance.getInstanceId(), task.getId(),
                            WorkflowEventType.TASK_CANCELLED,
                            "Task cancelled — workflow cancellation requested"));
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

    public LoggingService getLoggingService() {
        return loggingService;
    }
}