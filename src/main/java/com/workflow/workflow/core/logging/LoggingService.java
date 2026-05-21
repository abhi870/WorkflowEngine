package com.workflow.workflow.core.logging;


import com.workflow.workflow.core.constants.WorkflowEventType;

import java.util.List;

public interface LoggingService {


    void log(TaskExecutionLog entry);

    List<TaskExecutionLog> getLogsForTask(String taskInstanceId);

    int getAttemptCount(String taskInstanceId);

    List<TaskExecutionLog> getLogsForWorkflow(String workflowInstanceId);

    List<TaskExecutionLog> getFailedLogsForWorkflow(String workflowInstanceId);

    void logEvent(WorkflowLog event);

    List<WorkflowLog> getEventsForWorkflow(String workflowInstanceId);

    List<WorkflowLog> getEventsByType(String workflowInstanceId, WorkflowEventType type);

}