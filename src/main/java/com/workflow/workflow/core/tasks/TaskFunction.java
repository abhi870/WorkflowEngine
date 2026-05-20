package com.workflow.workflow.core.tasks;

@FunctionalInterface
public interface TaskFunction {
    void execute() throws Exception;
}