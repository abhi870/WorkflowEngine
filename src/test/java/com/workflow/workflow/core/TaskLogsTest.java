package com.workflow.workflow.core;

import com.workflow.workflow.core.constants.TaskStatus;
import com.workflow.workflow.core.logging.TaskExecutionLog;
import com.workflow.workflow.core.model.RetryPolicy;
import com.workflow.workflow.core.model.Task;
import com.workflow.workflow.core.model.Workflow;
import com.workflow.workflow.core.model.WorkflowInstance;
import com.workflow.workflow.core.tasks.FailOnceThenSucceedTask;
import com.workflow.workflow.core.tasks.PrintTask;
import com.workflow.workflow.support.WorkflowEngineTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


class TaskLogsTest extends WorkflowEngineTestBase {

    @Test
    void taskLogs_successfulTask_hasOneSuccessLog() throws Exception {
        Workflow wf = new Workflow("wf-task-logs-success");
        wf.addTask(new Task("A", "Task A", PRINT, new PrintTask("A done", 10)));

        WorkflowInstance wfi = engine.submit(wf);

        String tiId = wfi.findByTaskId("A").getInstanceId();
        List<TaskExecutionLog> logs = loggingService.getLogsForTask(tiId);

        System.out.println("Task A logs: " + logs);

        assertEquals(1, logs.size(), "expected exactly one execution log");
        TaskExecutionLog log = logs.get(0);
        assertEquals(TaskStatus.SUCCESS, log.getStatus());
        assertEquals(1, log.getAttempt());
        assertNotNull(log.getStartTime());
        assertNotNull(log.getEndTime());
        assertNull(log.getErrorMessage(), "no error message on success");
    }

    @Test
    void taskLogs_retriedTask_hasFailedThenSuccessLogs() throws Exception {
        Workflow wf = new Workflow("wf-task-logs-retry");
        wf.addTask(new Task("B", "Task B",
                Set.of(),
                FAIL_ONCE,
                new FailOnceThenSucceedTask("B"),
                new RetryPolicy(3, 50)));

        WorkflowInstance wfi = engine.submit(wf);

        String tiId = wfi.findByTaskId("B").getInstanceId();
        List<TaskExecutionLog> logs = loggingService.getLogsForTask(tiId);

        System.out.println("Task B attempt logs:");
        for (TaskExecutionLog log : logs) {
            System.out.println("  attempt=" + log.getAttempt()
                    + " status=" + log.getStatus()
                    + " err=" + log.getErrorMessage());
        }

        assertEquals(2, logs.size(), "expected 1 failed + 1 success log");
        assertEquals(TaskStatus.FAILED, logs.get(0).getStatus());
        assertEquals(1, logs.get(0).getAttempt());
        assertNotNull(logs.get(0).getErrorMessage());
        assertTrue(logs.get(0).getErrorMessage().contains("attempt 1"));

        assertEquals(TaskStatus.SUCCESS, logs.get(1).getStatus());
        assertEquals(2, logs.get(1).getAttempt());

        assertEquals(2, loggingService.getAttemptCount(tiId),
                "getAttemptCount should match number of attempts");
    }
}
