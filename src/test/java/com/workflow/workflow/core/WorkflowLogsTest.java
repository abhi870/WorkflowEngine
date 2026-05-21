package com.workflow.workflow.core;

import com.workflow.workflow.core.constants.WorkflowEventType;
import com.workflow.workflow.core.logging.WorkflowLog;
import com.workflow.workflow.core.model.Task;
import com.workflow.workflow.core.model.Workflow;
import com.workflow.workflow.core.model.WorkflowInstance;
import com.workflow.workflow.core.tasks.PrintTask;
import com.workflow.workflow.support.WorkflowEngineTestBase;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;


class WorkflowLogsTest extends WorkflowEngineTestBase {

    @Test
    void workflowLogs_emitsLifecycleEvents() throws Exception {
        Workflow wf = new Workflow("wf-workflow-logs");
        wf.addTask(new Task("A", "Task A", PRINT, new PrintTask("A done", 10)));
        wf.addTask(new Task("B", "Task B", Set.of("A"), PRINT, new PrintTask("B done", 10)));

        WorkflowInstance wfi = engine.submit(wf);

        List<WorkflowLog> events = loggingService.getEventsForWorkflow(wfi.getInstanceId());

        System.out.println("Workflow '" + wf.getId() + "' events:");
        for (WorkflowLog e : events) {
            System.out.println("  " + e.getLoggedAt() + " " + e.getEventType() + " — " + e.getMessage());
        }

        assertFalse(events.isEmpty(), "expected workflow events to be logged");

        EnumSet<WorkflowEventType> types = EnumSet.noneOf(WorkflowEventType.class);
        for (WorkflowLog e : events) types.add(e.getEventType());

        assertTrue(types.contains(WorkflowEventType.WORKFLOW_SUBMITTED));
        assertTrue(types.contains(WorkflowEventType.WORKFLOW_VALIDATED));
        assertTrue(types.contains(WorkflowEventType.LEVEL_STARTED));
        assertTrue(types.contains(WorkflowEventType.WORKFLOW_COMPLETED));

        int submittedIdx = indexOf(events, WorkflowEventType.WORKFLOW_SUBMITTED);
        int validatedIdx = indexOf(events, WorkflowEventType.WORKFLOW_VALIDATED);
        int completedIdx = indexOf(events, WorkflowEventType.WORKFLOW_COMPLETED);
        assertTrue(submittedIdx < validatedIdx, "submitted must come before validated");
        assertTrue(validatedIdx < completedIdx, "validated must come before completed");
    }

    @Test
    void workflowLogs_filterByType_returnsOnlyMatching() throws Exception {
        Workflow wf = new Workflow("wf-event-filter");
        wf.addTask(new Task("A", "Task A", PRINT, new PrintTask("A done", 5)));
        wf.addTask(new Task("B", "Task B", PRINT, new PrintTask("B done", 5)));
        wf.addTask(new Task("C", "Task C", Set.of("A", "B"), PRINT, new PrintTask("C done", 5)));

        WorkflowInstance wfi = engine.submit(wf);

        List<WorkflowLog> levelStarted = loggingService.getEventsByType(
                wfi.getInstanceId(), WorkflowEventType.LEVEL_STARTED);
        System.out.println("LEVEL_STARTED events: " + levelStarted.size());
        levelStarted.forEach(e -> System.out.println("  " + e.getMessage()));

        assertTrue(levelStarted.size() >= 2,
                "expected >= 2 LEVEL_STARTED events, got " + levelStarted.size());
        for (WorkflowLog e : levelStarted) {
            assertEquals(WorkflowEventType.LEVEL_STARTED, e.getEventType());
        }
    }


    private static int indexOf(List<WorkflowLog> events, WorkflowEventType type) {
        for (int i = 0; i < events.size(); i++) {
            if (events.get(i).getEventType() == type) return i;
        }
        return -1;
    }
}
