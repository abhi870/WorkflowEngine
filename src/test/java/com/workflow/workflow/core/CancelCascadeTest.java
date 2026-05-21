package com.workflow.workflow.core;

import com.workflow.workflow.core.constants.TaskStatus;
import com.workflow.workflow.core.constants.WorkflowStatus;
import com.workflow.workflow.core.model.Task;
import com.workflow.workflow.core.model.Workflow;
import com.workflow.workflow.core.model.WorkflowInstance;
import com.workflow.workflow.core.tasks.PrintTask;
import com.workflow.workflow.support.WorkflowEngineTestBase;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;


class CancelCascadeTest extends WorkflowEngineTestBase {

    @Test
    void testCancel_marksDownstreamTasksCancelled() throws Exception {
        Workflow wf = new Workflow("wf-cancel-cascade");
        wf.addTask(new Task("A", "Task A", PRINT, new PrintTask("A done", 300)));
        wf.addTask(new Task("B", "Task B", Set.of("A"), PRINT, new PrintTask("B done")));
        wf.addTask(new Task("C", "Task C", Set.of("B"), PRINT, new PrintTask("C done")));

        Thread canceller = new Thread(() -> {
            sleep(100);                       // let A start running
            engine.cancelWorkflow(wf.getId());
        });
        canceller.start();

        WorkflowInstance wfi = engine.submit(wf);
        canceller.join();

        assertEquals(TaskStatus.SUCCESS, statusOf(wfi, "A"));
        assertEquals(TaskStatus.CANCELLED, statusOf(wfi, "B"));
        assertEquals(TaskStatus.CANCELLED, statusOf(wfi, "C"));
        assertEquals(WorkflowStatus.CANCELLED, wfi.getStatus());
    }
}
