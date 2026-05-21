package com.workflow.workflow.core;

import com.workflow.workflow.core.constants.TaskStatus;
import com.workflow.workflow.core.logging.TaskExecutionLog;
import com.workflow.workflow.core.model.RetryPolicy;
import com.workflow.workflow.core.model.Task;
import com.workflow.workflow.core.model.Workflow;
import com.workflow.workflow.core.model.WorkflowInstance;
import com.workflow.workflow.core.tasks.FailOnceThenSucceedTask;
import com.workflow.workflow.core.tasks.FailingTask;
import com.workflow.workflow.core.tasks.PrintTask;
import com.workflow.workflow.support.WorkflowEngineTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


class RetryTest extends WorkflowEngineTestBase {

    @Test
    void retry_taskRecoversAfterOneFailure() throws Exception {
        Workflow wf = new Workflow("wf-retry-success");
        wf.addTask(new Task("A", "Task A", PRINT, new PrintTask("A done", 20)));
        wf.addTask(new Task("B", "Task B",
                Set.of("A"),
                FAIL_ONCE,
                new FailOnceThenSucceedTask("B"),
                new RetryPolicy(3, 50)));   // 3 attempts, 50ms backoff
        wf.addTask(new Task("C", "Task C", Set.of("B"), PRINT, new PrintTask("C done")));

        WorkflowInstance wfi = engine.submit(wf);

        assertEquals(TaskStatus.SUCCESS, statusOf(wfi, "A"));
        assertEquals(TaskStatus.SUCCESS, statusOf(wfi, "B"));
        assertEquals(TaskStatus.SUCCESS, statusOf(wfi, "C"));

        // Verify the attempt history through the logging service.
        String bTiId = wfi.findByTaskId("B").getInstanceId();
        List<TaskExecutionLog> bLogs = loggingService.getLogsForTask(bTiId);
        assertEquals(2, bLogs.size());
        assertEquals(TaskStatus.FAILED, bLogs.get(0).getStatus());
        assertEquals(TaskStatus.SUCCESS, bLogs.get(1).getStatus());
    }

    @Test
    void retry_exhaustsAttempts_taskFailsAndCascades() throws Exception {
        Workflow wf = new Workflow("wf-retry-exhausted");
        wf.addTask(new Task("A", "Task A", PRINT, new PrintTask("A done", 20)));
        wf.addTask(new Task("B", "Task B",
                Set.of("A"),
                FAIL,
                new FailingTask("permanently broken"),
                new RetryPolicy(2, 20)));   // 2 attempts total — both will fail
        wf.addTask(new Task("C", "Task C", Set.of("B"), PRINT, new PrintTask("C done")));

        WorkflowInstance wfi = engine.submit(wf);

        assertEquals(TaskStatus.SUCCESS, statusOf(wfi, "A"));
        assertEquals(TaskStatus.FAILED, statusOf(wfi, "B"));
        assertEquals(TaskStatus.SKIPPED, statusOf(wfi, "C"));

        String bTiId = wfi.findByTaskId("B").getInstanceId();
        List<TaskExecutionLog> bLogs = loggingService.getLogsForTask(bTiId);
        assertEquals(2, bLogs.size());
        for (TaskExecutionLog log : bLogs) {
            assertEquals(TaskStatus.FAILED, log.getStatus());
            assertNotNull(log.getErrorMessage());
            assertTrue(log.getErrorMessage().contains("permanently broken"));
        }
    }
}
