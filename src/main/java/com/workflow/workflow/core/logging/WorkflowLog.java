package com.workflow.workflow.core.logging;

import com.workflow.workflow.core.constants.WorkflowEventType;
import lombok.Getter;
import lombok.ToString;

import java.time.Instant;

@Getter
@ToString
public class WorkflowLog {


    private final String workflowInstanceId;
    private final String workflowId;
    private final String taskInstanceId;   // null for workflow-level events
    private final String taskId;           // null for workflow-level events
    private final WorkflowEventType eventType;
    private final String message;
    private final String loggedAt;

    public WorkflowLog(String workflowInstanceId, String workflowId,
                       String taskInstanceId, String taskId,
                       WorkflowEventType eventType, String message) {
        this.workflowInstanceId = workflowInstanceId;
        this.workflowId = workflowId;
        this.taskInstanceId = taskInstanceId;
        this.taskId = taskId;
        this.eventType = eventType;
        this.message = message;
        this.loggedAt = Instant.now().toString();
    }


    public static WorkflowLog workflowEvent(String workflowInstanceId, String workflowId,
                                            WorkflowEventType eventType, String message) {
        return new WorkflowLog(workflowInstanceId, workflowId, null, null, eventType, message);
    }

    public static WorkflowLog taskEvent(String workflowInstanceId, String workflowId,
                                        String taskInstanceId, String taskId,
                                        WorkflowEventType eventType, String message) {
        return new WorkflowLog(workflowInstanceId, workflowId,
                taskInstanceId, taskId, eventType, message);
    }


}