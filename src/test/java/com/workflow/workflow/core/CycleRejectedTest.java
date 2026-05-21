package com.workflow.workflow.core;

import com.workflow.workflow.core.model.Task;
import com.workflow.workflow.core.model.Workflow;
import com.workflow.workflow.core.tasks.PrintTask;
import com.workflow.workflow.support.WorkflowEngineTestBase;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


class CycleRejectedTest extends WorkflowEngineTestBase {

    @Test
    void testCycleRejected_twoNodeCycle() {
        Workflow wf = new Workflow("wf-cycle-2");
        wf.addTask(new Task("A", "Task A", Set.of("B"), PRINT, new PrintTask("A done")));
        wf.addTask(new Task("B", "Task B", Set.of("A"), PRINT, new PrintTask("B done")));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> engine.submit(wf),
                "submit() must throw on cyclic DAG");
        assertTrue(ex.getMessage().toLowerCase().contains("cycle"),
                "exception message should mention cycle, was: " + ex.getMessage());
    }

    @Test
    void testCycleRejected_threeNodeCycle() {
        Workflow wf = new Workflow("wf-cycle-3");
        wf.addTask(new Task("A", "Task A", Set.of("C"), PRINT, new PrintTask("A done")));
        wf.addTask(new Task("B", "Task B", Set.of("A"), PRINT, new PrintTask("B done")));
        wf.addTask(new Task("C", "Task C", Set.of("B"), PRINT, new PrintTask("C done")));

        assertThrows(IllegalStateException.class, () -> engine.submit(wf));
    }
}
