package com.workflow.workflow.core;

import com.workflow.workflow.core.constants.TaskStatus;
import com.workflow.workflow.core.constants.WorkflowStatus;
import com.workflow.workflow.core.model.Task;
import com.workflow.workflow.core.model.Workflow;
import com.workflow.workflow.core.model.WorkflowInstance;
import com.workflow.workflow.core.tasks.FailingTask;
import com.workflow.workflow.core.tasks.PrintTask;
import com.workflow.workflow.support.WorkflowEngineTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;


class DeepCascadeTest extends WorkflowEngineTestBase {

    @Test
    void testDeepCascade_propagatesSkipThroughChain() throws Exception {
        Workflow wf = new Workflow("wf-deep-cascade");
        wf.addTask(new Task("A", "Task A", PRINT, new PrintTask("A done", 30)));
        wf.addTask(new Task("B", "Task B", Set.of("A"), FAIL, new FailingTask("B exploded")));
        wf.addTask(new Task("C", "Task C", Set.of("B"), PRINT, new PrintTask("C done")));
        wf.addTask(new Task("D", "Task D", Set.of("C"), PRINT, new PrintTask("D done")));

        WorkflowInstance wfi = engine.submit(wf);

        assertEquals(TaskStatus.SUCCESS, statusOf(wfi, "A"));
        assertEquals(TaskStatus.FAILED, statusOf(wfi, "B"));
        assertEquals(TaskStatus.SKIPPED, statusOf(wfi, "C"));
        assertEquals(TaskStatus.SKIPPED, statusOf(wfi, "D"));
        assertEquals(WorkflowStatus.FAILED, wfi.getStatus());
    }

    @Test
    void testDeepCascade_diamondFanOut() throws Exception {
        //        A
        //        ↓
        //        B (fails)
        //       / \
        //      C   D
        //       \ /
        //        E
        Workflow wf = new Workflow("wf-deep-diamond");
        wf.addTask(new Task("A", "Task A", PRINT, new PrintTask("A done", 20)));
        wf.addTask(new Task("B", "Task B", Set.of("A"), FAIL, new FailingTask("B exploded")));
        wf.addTask(new Task("C", "Task C", Set.of("B"), PRINT, new PrintTask("C done")));
        wf.addTask(new Task("D", "Task D", Set.of("B"), PRINT, new PrintTask("D done")));
        wf.addTask(new Task("E", "Task E", Set.of("C", "D"), PRINT, new PrintTask("E done")));

        WorkflowInstance wfi = engine.submit(wf);

        assertEquals(TaskStatus.SUCCESS, statusOf(wfi, "A"));
        assertEquals(TaskStatus.FAILED, statusOf(wfi, "B"));
        assertEquals(TaskStatus.SKIPPED, statusOf(wfi, "C"));
        assertEquals(TaskStatus.SKIPPED, statusOf(wfi, "D"));
        assertEquals(TaskStatus.SKIPPED, statusOf(wfi, "E"));
        assertEquals(WorkflowStatus.FAILED, wfi.getStatus());
    }
}
