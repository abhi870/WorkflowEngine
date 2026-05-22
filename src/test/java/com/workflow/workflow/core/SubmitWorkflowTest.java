package com.workflow.workflow.core;

import com.workflow.workflow.core.constants.TaskStatus;
import com.workflow.workflow.core.constants.WorkflowStatus;
import com.workflow.workflow.core.model.Task;
import com.workflow.workflow.core.model.Workflow;
import com.workflow.workflow.core.model.WorkflowInstance;
import com.workflow.workflow.core.tasks.PrintTask;
import com.workflow.workflow.support.WorkflowEngineTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


class SubmitWorkflowTest extends WorkflowEngineTestBase {

    @Test
    void testLinear() throws Exception {
        Workflow wf = new Workflow("wf-submit-linear");
        wf.addTask(new Task("A", "Task A", PRINT, new PrintTask("A done", 50)));
        wf.addTask(new Task("B", "Task B", Set.of("A"), PRINT, new PrintTask("B done", 50)));
        wf.addTask(new Task("C", "Task C", Set.of("B"), PRINT, new PrintTask("C done", 50)));

        WorkflowInstance wfi = engine.submit(wf);

        assertNotNull(wfi.getInstanceId());
        assertEquals(WorkflowStatus.SUCCESS, wfi.getStatus());
        assertEquals(TaskStatus.SUCCESS, statusOf(wfi, "A"));
        assertEquals(TaskStatus.SUCCESS, statusOf(wfi, "B"));
        assertEquals(TaskStatus.SUCCESS, statusOf(wfi, "C"));
        assertNotNull(wfi.getEndTime());
    }

    @Test
    void testParallel() throws Exception {
        Workflow wf = new Workflow("wf-submit-parallel");

        wf.addTask(new Task("A", "Task A", PRINT, new PrintTask("A done", 200)));
        wf.addTask(new Task("B", "Task B", PRINT, new PrintTask("B done", 200)));
        wf.addTask(new Task("C", "Task C", Set.of("A", "B"), PRINT, new PrintTask("C done", 100)));

        long start = System.currentTimeMillis();
        WorkflowInstance wfi = engine.submit(wf);
        long elapsedMs = System.currentTimeMillis() - start;

        assertEquals(WorkflowStatus.SUCCESS, wfi.getStatus());
        assertEquals(TaskStatus.SUCCESS, statusOf(wfi, "A"));
        assertEquals(TaskStatus.SUCCESS, statusOf(wfi, "B"));
        assertEquals(TaskStatus.SUCCESS, statusOf(wfi, "C"));

        assertTrue(elapsedMs < 450);
    }
}
