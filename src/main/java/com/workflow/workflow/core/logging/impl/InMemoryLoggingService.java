package com.workflow.workflow.core.logging.impl;


import com.workflow.workflow.core.constants.TaskStatus;
import com.workflow.workflow.core.constants.WorkflowEventType;
import com.workflow.workflow.core.logging.LoggingService;
import com.workflow.workflow.core.logging.TaskExecutionLog;
import com.workflow.workflow.core.logging.WorkflowLog;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class InMemoryLoggingService implements LoggingService {

    private final ConcurrentHashMap<String, CopyOnWriteArrayList<TaskExecutionLog>>
            taskIndex = new ConcurrentHashMap<>();  // taskInstanceId → logs
    private final ConcurrentHashMap<String, CopyOnWriteArrayList<TaskExecutionLog>>
            attemptIndex = new ConcurrentHashMap<>();  // workflowInstanceId → logs

    private final ConcurrentHashMap<String, CopyOnWriteArrayList<WorkflowLog>>
            eventIndex = new ConcurrentHashMap<>();  // workflowInstanceId → events


    @Override
    public void log(TaskExecutionLog entry) {
        taskIndex
                .computeIfAbsent(entry.getTaskInstanceId(), k -> new CopyOnWriteArrayList<>())
                .add(entry);
        attemptIndex
                .computeIfAbsent(entry.getWorkflowInstanceId(), k -> new CopyOnWriteArrayList<>())
                .add(entry);
    }


    @Override
    public void logEvent(WorkflowLog event) {
        eventIndex
                .computeIfAbsent(event.getWorkflowInstanceId(), k -> new CopyOnWriteArrayList<>())
                .add(event);
    }

    @Override
    public List<TaskExecutionLog> getLogsForTask(String taskInstanceId) {
        CopyOnWriteArrayList<TaskExecutionLog> logs = taskIndex.get(taskInstanceId);
        if (logs == null) return List.of();
        return logs.stream()
                .sorted(Comparator.comparingInt(TaskExecutionLog::getAttempt))
                .collect(Collectors.toList());
    }

    @Override
    public int getAttemptCount(String taskInstanceId) {
        CopyOnWriteArrayList<TaskExecutionLog> logs = taskIndex.get(taskInstanceId);
        return logs == null ? 0 : logs.size();
    }

    @Override
    public List<TaskExecutionLog> getLogsForWorkflow(String workflowInstanceId) {
        CopyOnWriteArrayList<TaskExecutionLog> logs = attemptIndex.get(workflowInstanceId);
        if (logs == null) return List.of();
        return logs.stream()
                .sorted(Comparator.comparing(TaskExecutionLog::getLoggedAt))
                .collect(Collectors.toList());
    }

    @Override
    public List<TaskExecutionLog> getFailedLogsForWorkflow(String workflowInstanceId) {
        return getLogsForWorkflow(workflowInstanceId).stream()
                .filter(log -> log.getStatus() == TaskStatus.FAILED)
                .collect(Collectors.toList());
    }

    @Override
    public List<WorkflowLog> getEventsForWorkflow(String workflowInstanceId) {
        CopyOnWriteArrayList<WorkflowLog> events = eventIndex.get(workflowInstanceId);
        if (events == null) return List.of();
        return events.stream()
                .sorted(Comparator.comparing(WorkflowLog::getLoggedAt))
                .collect(Collectors.toList());
    }

    @Override
    public List<WorkflowLog> getEventsByType(String workflowInstanceId,
                                             WorkflowEventType type) {
        return getEventsForWorkflow(workflowInstanceId).stream()
                .filter(e -> e.getEventType() == type)
                .collect(Collectors.toList());
    }


    private long getDurationMs(TaskExecutionLog log) {
        if (log.getStartTime() == null || log.getEndTime() == null) return 0;
        return java.time.Duration.between(
                java.time.Instant.parse(log.getStartTime()),
                java.time.Instant.parse(log.getEndTime())
        ).toMillis();
    }
}