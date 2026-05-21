package com.workflow.workflow.core;

import com.workflow.workflow.core.constants.TaskStatus;
import com.workflow.workflow.core.model.Task;
import com.workflow.workflow.core.model.TaskInstance;
import com.workflow.workflow.core.model.Workflow;
import com.workflow.workflow.core.model.WorkflowInstance;
import com.workflow.workflow.core.tasks.FailingTask;
import com.workflow.workflow.core.tasks.PrintTask;
import com.workflow.workflow.support.WorkflowEngineTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


class GetTaskStatusTest extends WorkflowEngineTestBase {

    @Test
    void getTaskStatus_returnsSuccess_forCompletedTask() throws Exception {
        Workflow wf = new Workflow("wf-status-success");
        wf.addTask(new Task("A", "Task A", PRINT, new PrintTask("A done", 20)));

        WorkflowInstance wfi = engine.submit(wf);
        TaskInstance ti = wfi.findByTaskId("A");

        assertEquals(TaskStatus.SUCCESS, engine.getTaskInstanceStatus(ti.getInstanceId()));
    }

    @Test
    void getTaskStatus_reportsFailedAndSkipped() throws Exception {
        Workflow wf = new Workflow("wf-status-mixed");
        wf.addTask(new Task("A", "Task A", PRINT, new PrintTask("A done", 20)));
        wf.addTask(new Task("B", "Task B", Set.of("A"), FAIL, new FailingTask("boom")));
        wf.addTask(new Task("C", "Task C", Set.of("B"), PRINT, new PrintTask("C done")));

        WorkflowInstance wfi = engine.submit(wf);

        assertEquals(TaskStatus.SUCCESS,
                engine.getTaskInstanceStatus(wfi.findByTaskId("A").getInstanceId()));
        assertEquals(TaskStatus.FAILED,
                engine.getTaskInstanceStatus(wfi.findByTaskId("B").getInstanceId()));
        assertEquals(TaskStatus.SKIPPED,
                engine.getTaskInstanceStatus(wfi.findByTaskId("C").getInstanceId()));
    }

    @Test
    void getTaskStatus_throws_forUnknownInstance() {
        assertThrows(IllegalArgumentException.class,
                () -> engine.getTaskInstanceStatus("does-not-exist"));
    }
}
