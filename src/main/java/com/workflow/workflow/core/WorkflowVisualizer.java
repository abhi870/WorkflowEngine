package com.workflow.workflow.core;

import com.workflow.workflow.core.model.Task;
import com.workflow.workflow.core.model.Workflow;
import com.workflow.workflow.core.model.WorkflowInstance;
import com.workflow.workflow.core.constants.TaskStatus;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class WorkflowVisualizer {

    private WorkflowVisualizer() {
    }

    /**
     * Export with runtime status — pass WorkflowInstance for coloured nodes.
     * Call before submit() for a PENDING plan, after submit() for final result.
     */
    public static void export(Workflow workflow, WorkflowInstance wfInstance,
                              Path outputPath) throws IOException {
        String dot = toDot(workflow, wfInstance);
        Files.writeString(outputPath, dot);
        System.out.println("[Visualizer] DOT file written to: " + outputPath.toAbsolutePath());
    }

    /**
     * Export definition only — all nodes shown as PENDING (white).
     * Useful for visualising the workflow structure before execution.
     */
    public static void export(Workflow workflow, Path outputPath) throws IOException {
        String dot = toDot(workflow, null);
        Files.writeString(outputPath, dot);
        System.out.println("[Visualizer] DOT file written to: " + outputPath.toAbsolutePath());
    }

    public static String toDot(Workflow workflow, WorkflowInstance wfInstance) {
        StringBuilder sb = new StringBuilder();

        String graphId = workflow.getId().replace("-", "_");
        sb.append("digraph ").append(graphId).append(" {\n");
        sb.append("    rankdir=LR;\n");
        sb.append("    node [shape=box fontname=Helvetica];\n");
        sb.append("\n");

        // Nodes — status resolved from WorkflowInstance if available
        for (Task task : workflow.getTasks()) {
            TaskStatus status = resolveStatus(task, wfInstance);
            sb.append("    ")
                    .append(quoted(task.getId()))
                    .append(" [label=").append(label(task, status))
                    .append(" style=filled fillcolor=").append(fillColor(status))
                    .append("];\n");
        }

        sb.append("\n");

        // Edges
        for (Task task : workflow.getTasks()) {
            for (String depId : task.getDependencies()) {
                sb.append("    ")
                        .append(quoted(depId))
                        .append(" -> ")
                        .append(quoted(task.getId()))
                        .append(";\n");
            }
        }

        sb.append("}\n");
        return sb.toString();
    }

    // ── Private ───────────────────────────────────────────────────────────────

    private static TaskStatus resolveStatus(Task task, WorkflowInstance wfInstance) {
        if (wfInstance == null) return TaskStatus.PENDING;
        try {
            return wfInstance.findByTaskId(task.getId()).getStatus();
        } catch (IllegalArgumentException e) {
            return TaskStatus.PENDING;  // task instance not found — treat as pending
        }
    }

    private static String label(Task task, TaskStatus status) {
        return "\"" + task.getId() + "\\n"
                + task.getTaskName() + "\\n"
                + status + "\"";
    }

    private static String fillColor(TaskStatus status) {
        return switch (status) {
            case PENDING -> "\"#FFFFFF\"";   // white  — not yet started
            case RUNNING -> "\"#FFD700\"";   // yellow — currently executing
            case SUCCESS -> "\"#90EE90\"";   // green  — completed successfully
            case FAILED -> "\"#FF6B6B\"";   // red    — exhausted retries
            case SKIPPED -> "\"#D3D3D3\"";   // grey   — upstream dependency failed
            case CANCELLED -> "\"#FFA500\"";   // orange — workflow was cancelled
        };
    }

    private static String quoted(String id) {
        return "\"" + id + "\"";
    }
}