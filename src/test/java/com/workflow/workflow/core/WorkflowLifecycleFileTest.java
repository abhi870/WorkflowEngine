package com.workflow.workflow.core;

import com.workflow.workflow.core.constants.WorkflowEventType;
import com.workflow.workflow.core.logging.WorkflowLog;
import com.workflow.workflow.core.model.Task;
import com.workflow.workflow.core.model.Workflow;
import com.workflow.workflow.core.model.WorkflowInstance;
import com.workflow.workflow.core.tasks.PrintTask;
import com.workflow.workflow.support.WorkflowEngineTestBase;
import org.junit.jupiter.api.Test;

import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs a workflow, captures its lifecycle events from the LoggingService,
 * and writes them to a file in the current working directory (project root
 * when run via Maven / your IDE) — then asserts the file was produced and
 * contains the expected lifecycle markers. The file is left on disk after
 * the test so it can be inspected.
 */
class WorkflowLifecycleFileTest extends WorkflowEngineTestBase {

    private static final String OUTPUT_FILE = "workflow-lifecycle.log";

    @Test
    void writesWorkflowLifecycleEventsToFile() throws Exception {
        Workflow wf = new Workflow("wf-lifecycle-file");
        wf.addTask(new Task("A", "Task A", PRINT, new PrintTask("A done", 10)));
        wf.addTask(new Task("B", "Task B", Set.of("A"), PRINT, new PrintTask("B done", 10)));
        wf.addTask(new Task("C", "Task C", Set.of("B"), PRINT, new PrintTask("C done", 10)));

        WorkflowInstance wfi = engine.submit(wf);

        List<WorkflowLog> events = loggingService.getEventsForWorkflow(wfi.getInstanceId());
        assertFalse(events.isEmpty(), "expected at least one lifecycle event");

        // Resolve a path relative to the current working directory — when the
        // test runs via Maven this is the project root, alongside pom.xml.
        Path outFile = Path.of(OUTPUT_FILE).toAbsolutePath();
        try (BufferedWriter writer = Files.newBufferedWriter(outFile)) {
            writer.write("# Workflow: " + wf.getId() + " (instance " + wfi.getInstanceId() + ")");
            writer.newLine();
            for (WorkflowLog e : events) {
                writer.write(String.format("[%s] %s -- %s",
                        e.getLoggedAt(), e.getEventType(), e.getMessage()));
                writer.newLine();
            }
        }

        // Make the path visible in the test output for manual inspection.
        System.out.println("Wrote " + events.size()
                + " lifecycle events to: " + outFile.toAbsolutePath());

        // ── Assertions ────────────────────────────────────────────────────
        assertTrue(Files.exists(outFile), "lifecycle file should exist on disk");
        assertTrue(Files.size(outFile) > 0, "lifecycle file should not be empty");

        List<String> lines = Files.readAllLines(outFile);
        assertEquals(events.size() + 1, lines.size(),
                "expected one header line plus one line per event");
        assertTrue(lines.get(0).contains(wf.getId()),
                "header should reference the workflow id");

        // The file should record every key lifecycle marker.
        String body = String.join("\n", lines);
        assertTrue(body.contains(WorkflowEventType.WORKFLOW_SUBMITTED.name()),
                "file should mention WORKFLOW_SUBMITTED");
        assertTrue(body.contains(WorkflowEventType.WORKFLOW_VALIDATED.name()),
                "file should mention WORKFLOW_VALIDATED");
        assertTrue(body.contains(WorkflowEventType.LEVEL_STARTED.name()),
                "file should mention LEVEL_STARTED");
        assertTrue(body.contains(WorkflowEventType.WORKFLOW_COMPLETED.name()),
                "file should mention WORKFLOW_COMPLETED");
    }
}
