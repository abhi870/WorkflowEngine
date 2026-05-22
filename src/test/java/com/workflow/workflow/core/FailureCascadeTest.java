package com.workflow.workflow.core;

import com.workflow.workflow.core.constants.TaskStatus;
import com.workflow.workflow.core.constants.WorkflowStatus;
import com.workflow.workflow.core.logging.TaskExecutionLog;
import com.workflow.workflow.core.model.Task;
import com.workflow.workflow.core.model.Workflow;
import com.workflow.workflow.core.model.WorkflowInstance;
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


class FailureCascadeTest extends WorkflowEngineTestBase {

//    @Test
//    void testingFailureCascade_oneHop() throws Exception {
//        Workflow wf = new Workflow("wf-cascade-1hop");
//        wf.addTask(new Task("A", "Task A", PRINT, new PrintTask("A done", 30)));
//        wf.addTask(new Task("B", "Task B", Set.of("A"), FAIL, new FailingTask("B exploded")));
//        wf.addTask(new Task("C", "Task C", Set.of("B"), PRINT, new PrintTask("C done")));
//
//        WorkflowInstance wfi = engine.submit(wf);
//
//        assertEquals(TaskStatus.SUCCESS, statusOf(wfi, "A"));
//        assertEquals(TaskStatus.FAILED, statusOf(wfi, "B"));
//        assertEquals(TaskStatus.SKIPPED, statusOf(wfi, "C"));
//        assertEquals(WorkflowStatus.FAILED, wfi.getStatus());
//        assertTrue(wfi.anyFailed());
//
//        String bTiId = wfi.findByTaskId("B").getInstanceId();
//        List<TaskExecutionLog> bLogs = loggingService.getLogsForTask(bTiId);
//        assertTrue(bLogs.size() >= 1);
//        TaskExecutionLog last = bLogs.get(bLogs.size() - 1);
//        assertEquals(TaskStatus.FAILED, last.getStatus());
//        assertNotNull(last.getErrorMessage());
//        assertTrue(last.getErrorMessage().contains("B exploded"));
//    }

    @Test
    void testingFailureCascade_doesNotAffectIndependentBranch() throws Exception {
        Workflow wf = new Workflow("wf-cascade-sibling");
        wf.addTask(new Task("A", "Task A", PRINT, new PrintTask("A done", 30)));
        wf.addTask(new Task("B", "Task B ", Set.of("A"), FAIL, new FailingTask("nope")));
        wf.addTask(new Task("C", "Task C", Set.of("B"), PRINT, new PrintTask("C done")));
        wf.addTask(new Task("D", "Task D", Set.of("A"), PRINT, new PrintTask("D done", 20)));
        wf.addTask(new Task("E", "Task E", Set.of("D"), PRINT, new PrintTask("E done", 20)));

        WorkflowInstance wfi = engine.submit(wf);

        assertEquals(TaskStatus.SUCCESS, statusOf(wfi, "A"));
        assertEquals(TaskStatus.FAILED, statusOf(wfi, "B"));
        assertEquals(TaskStatus.SKIPPED, statusOf(wfi, "C"));
        assertEquals(TaskStatus.SUCCESS, statusOf(wfi, "D"));
        assertEquals(TaskStatus.SUCCESS, statusOf(wfi, "E"));
        assertEquals(WorkflowStatus.FAILED, wfi.getStatus());
    }
}
