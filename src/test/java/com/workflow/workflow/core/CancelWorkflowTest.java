package com.workflow.workflow.core;

import com.workflow.workflow.core.constants.TaskStatus;
import com.workflow.workflow.core.constants.WorkflowEventType;
import com.workflow.workflow.core.constants.WorkflowStatus;
import com.workflow.workflow.core.logging.WorkflowLog;
import com.workflow.workflow.core.model.Task;
import com.workflow.workflow.core.model.Workflow;
import com.workflow.workflow.core.model.WorkflowInstance;
import com.workflow.workflow.core.tasks.PrintTask;
import com.workflow.workflow.support.WorkflowEngineTestBase;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;


class CancelWorkflowTest extends WorkflowEngineTestBase {

    @Test
    void cancelWorkflow_marksWorkflowCancelled_andEmitsCancelEvent() throws Exception {
        Workflow wf = new Workflow("wf-cancel-api");
        wf.addTask(new Task("A", "Task A", PRINT, new PrintTask("A done", 300)));
        wf.addTask(new Task("B", "Task B", Set.of("A"), PRINT, new PrintTask("B done")));

        Thread canceller = new Thread(() -> {
            sleep(100);
            engine.cancelWorkflow(wf.getId());
        });
        canceller.start();

        WorkflowInstance wfi = engine.submit(wf);
        canceller.join();

        assertEquals(WorkflowStatus.CANCELLED, wfi.getStatus());
        assertTrue(wfi.isCancelRequested(), "cancelRequested flag should be set");

        List<WorkflowLog> cancelEvents = loggingService.getEventsByType(
                wfi.getInstanceId(), WorkflowEventType.WORKFLOW_CANCEL_REQUESTED);
        assertFalse(cancelEvents.isEmpty(), "expected at least one cancel-requested event");
        assertEquals(wf.getId(), cancelEvents.get(0).getWorkflowId());
    }

    @Test
    void cancelWorkflow_unknownId_isNoOp() {
        engine.cancelWorkflow("no-such-workflow");
    }

    @Test
    void cancelWorkflow_afterCompletion_isIgnored() throws Exception {
        Workflow wf = new Workflow("wf-already-done");
        wf.addTask(new Task("A", "Task A", PRINT, new PrintTask("A done", 10)));

        WorkflowInstance wfi = engine.submit(wf);

        // After submit returns, the instance is no longer active — cancel is a no-op.
        engine.cancelWorkflow(wf.getId());

        assertEquals(WorkflowStatus.SUCCESS, wfi.getStatus());
        assertEquals(TaskStatus.SUCCESS, statusOf(wfi, "A"));
        assertFalse(wfi.isCancelRequested(), "cancel flag should not flip after completion");
    }
}
