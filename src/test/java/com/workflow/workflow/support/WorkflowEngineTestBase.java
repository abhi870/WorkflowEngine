package com.workflow.workflow.support;

import com.workflow.workflow.core.WorkflowEngine;
import com.workflow.workflow.core.logging.LoggingService;
import com.workflow.workflow.core.logging.impl.InMemoryLoggingService;
import com.workflow.workflow.core.model.WorkflowInstance;
import com.workflow.workflow.core.tasks.FailOnceThenSucceedTask;
import com.workflow.workflow.core.tasks.FailingTask;
import com.workflow.workflow.core.tasks.PrintTask;
import com.workflow.workflow.database.*;
import com.workflow.workflow.database.impl.InMemoryTaskInstanceRepository;
import com.workflow.workflow.database.impl.InMemoryTaskRepository;
import com.workflow.workflow.database.impl.InMemoryWorkflowRepository;
import com.workflow.workflow.database.impl.WorkflowServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import com.workflow.workflow.core.constants.TaskStatus;

public abstract class WorkflowEngineTestBase {

    public static final String PRINT = "com.workflow.workflow.core.tasks.PrintTask";
    public static final String FAIL = "com.workflow.workflow.core.tasks.FailingTask";
    public static final String FAIL_ONCE = "com.workflow.workflow.core.tasks.FailOnceThenSucceedTask";

    protected WorkflowEngine engine;
    protected LoggingService loggingService;
    protected WorkflowService workflowService;
    protected TaskRegistry taskRegistry;

    @BeforeEach
    void setUpEngine() {
        TaskRepository taskRepo = new InMemoryTaskRepository();
        TaskInstanceRepository taskInstanceRepo = new InMemoryTaskInstanceRepository();
        WorkflowRepository workflowRepo = new InMemoryWorkflowRepository(taskRepo);

        this.workflowService = new WorkflowServiceImpl(taskRepo, taskInstanceRepo, workflowRepo);
        this.taskRegistry = new TaskRegistry();
        taskRegistry.register(PrintTask.class);
        taskRegistry.register(FailingTask.class);
        taskRegistry.register(FailOnceThenSucceedTask.class);

        this.loggingService = new InMemoryLoggingService();
        this.engine = new WorkflowEngine(workflowService, taskRegistry, loggingService);
    }

    @AfterEach
    void tearDownEngine() {
        if (engine != null) engine.shutdownNow();
    }


    protected static TaskStatus statusOf(WorkflowInstance wfi, String taskId) {
        return wfi.findByTaskId(taskId).getStatus();
    }

    protected static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
