package com.workflow.workflow.core;

import com.workflow.workflow.entities.task.TaskStatus;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class WorkflowVisualizer {

    private WorkflowVisualizer() {}

    public static void export(Workflow workflow, Path outputPath) throws IOException {
        String dot = toDot(workflow);
        Files.writeString(outputPath, dot);
        System.out.println("[Visualizer] DOT file written to: " + outputPath.toAbsolutePath());
    }

    public static String toDot(Workflow workflow) {
        StringBuilder sb = new StringBuilder();

        String graphId = workflow.getId().replace("-", "_");
        sb.append("digraph ").append(graphId).append(" {\n");
        sb.append("    rankdir=LR;\n");
        sb.append("    node [shape=box fontname=Helvetica];\n");
        sb.append("\n");

        // Nodes
        for (Task task : workflow.getTasks()) {
            sb.append("    ")
                    .append(quoted(task.getId()))
                    .append(" [label=").append(label(task))
                    .append(" style=filled fillcolor=").append(fillColor(task.getStatus()))
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

    private static String label(Task task) {
        return "\"" + task.getId() + "\\n"
                + task.getTaskName() + "\\n"
                + task.getStatus() + "\"";
    }

    private static String fillColor(TaskStatus status) {
        return switch (status) {
            case PENDING   -> "\"#FFFFFF\"";   // white  — not yet started
            case RUNNING   -> "\"#FFD700\"";   // yellow — currently executing
            case SUCCESS   -> "\"#90EE90\"";   // green  — completed successfully
            case FAILED    -> "\"#FF6B6B\"";   // red    — exhausted retries
            case SKIPPED   -> "\"#D3D3D3\"";   // grey   — upstream dependency failed
            case CANCELLED -> "\"#FFA500\"";   // orange — workflow was cancelled
        };
    }

    private static String quoted(String id) {
        return "\"" + id + "\"";
    }
}