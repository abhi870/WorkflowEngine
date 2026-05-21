package com.workflow.workflow;

import com.workflow.workflow.core.WorkflowEngine;
import com.workflow.workflow.core.WorkflowVisualizer;
import com.workflow.workflow.core.constants.TaskStatus;
import com.workflow.workflow.core.constants.WorkflowStatus;
import com.workflow.workflow.core.logging.LoggingService;
import com.workflow.workflow.core.logging.TaskExecutionLog;
import com.workflow.workflow.core.logging.impl.InMemoryLoggingService;
import com.workflow.workflow.core.model.RetryPolicy;
import com.workflow.workflow.core.model.Task;
import com.workflow.workflow.core.model.Workflow;
import com.workflow.workflow.core.model.WorkflowInstance;
import com.workflow.workflow.core.tasks.FailOnceThenSucceedTask;
import com.workflow.workflow.core.tasks.FailingTask;
import com.workflow.workflow.core.tasks.PrintTask;
import com.workflow.workflow.database.TaskRegistry;
import com.workflow.workflow.database.WorkflowService;
import com.workflow.workflow.database.impl.InMemoryTaskInstanceRepository;
import com.workflow.workflow.database.impl.InMemoryTaskRepository;
import com.workflow.workflow.database.impl.InMemoryWorkflowRepository;
import com.workflow.workflow.database.impl.WorkflowServiceImpl;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

@SpringBootApplication
public class WorkflowApplication {

    static final String PRINT = "com.workflow.workflow.core.tasks.PrintTask";
    static final String FAIL = "com.workflow.workflow.core.tasks.FailingTask";

    public static void main(String[] args) {
        SpringApplication.run(WorkflowApplication.class, args);
    }

    @Bean
    public CommandLineRunner commandLineRunner() {
        return args -> {
            InMemoryTaskRepository taskRepo = new InMemoryTaskRepository();
            InMemoryTaskInstanceRepository taskInstanceRepo = new InMemoryTaskInstanceRepository();
            InMemoryWorkflowRepository workflowRepo = new InMemoryWorkflowRepository(taskRepo);
            WorkflowService workflowService = new WorkflowServiceImpl(taskRepo, taskInstanceRepo, workflowRepo);
            TaskRegistry taskRegistry = new TaskRegistry();
            taskRegistry.register(PrintTask.class);
            taskRegistry.register(FailingTask.class);
            LoggingService loggingService = new InMemoryLoggingService();
            WorkflowEngine engine = new WorkflowEngine(workflowService, taskRegistry, loggingService);

            testLinear(engine);
            testParallel(engine);
            testCycleRejected(engine);
            testMissingDep(engine);
            testFailureCascade(engine);
            testDeepCascade(engine);
            testCancel(engine);
            testRetry(engine);

            engine.shutdown();
            System.out.println("\n🎉 All tests passed.");
        };
    }

    static void testLinear(WorkflowEngine engine) throws Exception {
        System.out.println("\n══════════════════════════════════════");
        System.out.println(" TEST 1 — Linear: A -> B -> C");
        System.out.println("══════════════════════════════════════");

        Workflow wf = new Workflow("wf-linear");
        wf.addTask(new Task("A", "Task A", PRINT, new PrintTask("A done", 100)));
        wf.addTask(new Task("B", "Task B", Set.of("A"), PRINT, new PrintTask("B done", 100)));
        wf.addTask(new Task("C", "Task C", Set.of("B"), PRINT, new PrintTask("C done", 100)));

        WorkflowInstance wfi = engine.submit(wf);

        assert statusOf(wfi, "A") == TaskStatus.SUCCESS;
        assert statusOf(wfi, "B") == TaskStatus.SUCCESS;
        assert statusOf(wfi, "C") == TaskStatus.SUCCESS;
        System.out.println("✓ Test 1 passed");
        System.out.println("  WorkflowInstance: " + wfi);
    }

    static void testParallel(WorkflowEngine engine) throws Exception {
        System.out.println("\n══════════════════════════════════════");
        System.out.println(" TEST 2 — Parallel: A + B -> C");
        System.out.println("══════════════════════════════════════");

        Workflow wf = new Workflow("wf-parallel");
        wf.addTask(new Task("A", "Task A", PRINT, new PrintTask("A done", 200)));
        wf.addTask(new Task("B", "Task B", PRINT, new PrintTask("B done", 200)));
        wf.addTask(new Task("C", "Task C", Set.of("A", "B"), PRINT, new PrintTask("C done", 100)));

        WorkflowVisualizer.export(wf, Path.of("workflow-plan.dot"));   // all PENDING
        long start = System.currentTimeMillis();
        WorkflowInstance wfi = engine.submit(wf);
        long elapsed = System.currentTimeMillis() - start;
        WorkflowVisualizer.export(wf, wfi, Path.of("workflow-result.dot")); // with status

        System.out.println("[Test] Wall time: " + elapsed + "ms");
        assert elapsed < 450 : "Expected parallel execution, got " + elapsed + "ms";
        assert statusOf(wfi, "A") == TaskStatus.SUCCESS;
        assert statusOf(wfi, "B") == TaskStatus.SUCCESS;
        assert statusOf(wfi, "C") == TaskStatus.SUCCESS;
        System.out.println("✓ Test 2 passed — parallelism confirmed");
    }

    static void testCycleRejected(WorkflowEngine engine) {
        System.out.println("\n══════════════════════════════════════");
        System.out.println(" TEST 3 — Cycle: A -> B -> A");
        System.out.println("══════════════════════════════════════");

        Workflow wf = new Workflow("wf-cycle");
        wf.addTask(new Task("A", "Task A", Set.of("B"), PRINT, new PrintTask("A done")));
        wf.addTask(new Task("B", "Task B", Set.of("A"), PRINT, new PrintTask("B done")));

        try {
            engine.submit(wf);
            throw new AssertionError("Should have thrown for cycle");
        } catch (IllegalStateException e) {
            System.out.println("Correctly rejected: " + e.getMessage());
        } catch (Exception e) {
            throw new AssertionError("Wrong exception: " + e);
        }
        System.out.println("✓ Test 3 passed");
    }

    static void testMissingDep(WorkflowEngine engine) {
        System.out.println("\n══════════════════════════════════════");
        System.out.println(" TEST 4 — Unknown dep: B -> X");
        System.out.println("══════════════════════════════════════");

        Workflow wf = new Workflow("wf-missing");
        wf.addTask(new Task("A", "Task A", PRINT, new PrintTask("A done")));
        wf.addTask(new Task("B", "Task B", Set.of("X"), PRINT, new PrintTask("B done")));

        try {
            engine.submit(wf);
            throw new AssertionError("Should have thrown for missing dep");
        } catch (IllegalStateException e) {
            System.out.println("Correctly rejected: " + e.getMessage());
        } catch (Exception e) {
            throw new AssertionError("Wrong exception: " + e);
        }
        System.out.println("✓ Test 4 passed");
    }

    static void testFailureCascade(WorkflowEngine engine) throws Exception {
        System.out.println("\n══════════════════════════════════════");
        System.out.println(" TEST 5 — Cascade: A -> B(fail) -> C(skip)");
        System.out.println("══════════════════════════════════════");

        Workflow wf = new Workflow("wf-cascade");
        wf.addTask(new Task("A", "Task A", PRINT, new PrintTask("A done", 50)));
        wf.addTask(new Task("B", "Task B", Set.of("A"), FAIL, new FailingTask("B exploded")));
        wf.addTask(new Task("C", "Task C", Set.of("B"), PRINT, new PrintTask("C done")));

        WorkflowInstance wfi = engine.submit(wf);

        assert statusOf(wfi, "A") == TaskStatus.SUCCESS : "A should be SUCCESS";
        assert statusOf(wfi, "B") == TaskStatus.FAILED : "B should be FAILED";
        assert statusOf(wfi, "C") == TaskStatus.SKIPPED : "C should be SKIPPED";

        // Print logs for B — shows the failed attempt
        System.out.println("  Task B logs: " + wfi.findByTaskId("B").getLogs());
        System.out.println("✓ Test 5 passed");
    }

    static void testDeepCascade(WorkflowEngine engine) throws Exception {
        System.out.println("\n══════════════════════════════════════");
        System.out.println(" TEST 6 — Deep cascade: A -> B(fail) -> C -> D");
        System.out.println("══════════════════════════════════════");

        Workflow wf = new Workflow("wf-deep");
        wf.addTask(new Task("A", "Task A", PRINT, new PrintTask("A done", 50)));
        wf.addTask(new Task("B", "Task B", Set.of("A"), FAIL, new FailingTask("B exploded")));
        wf.addTask(new Task("C", "Task C", Set.of("B"), PRINT, new PrintTask("C done")));
        wf.addTask(new Task("D", "Task D", Set.of("C"), PRINT, new PrintTask("D done")));

        WorkflowInstance wfi = engine.submit(wf);

        assert statusOf(wfi, "A") == TaskStatus.SUCCESS : "A should be SUCCESS";
        assert statusOf(wfi, "B") == TaskStatus.FAILED : "B should be FAILED";
        assert statusOf(wfi, "C") == TaskStatus.SKIPPED : "C should be SKIPPED";
        assert statusOf(wfi, "D") == TaskStatus.SKIPPED : "D should be SKIPPED";
        System.out.println("✓ Test 6 passed");
    }

    static void testCancel(WorkflowEngine engine) throws Exception {
        System.out.println("\n══════════════════════════════════════");
        System.out.println(" TEST 7 — Cancel: A runs, B and C cancelled");
        System.out.println("══════════════════════════════════════");

        Workflow wf = new Workflow("wf-cancel");
        wf.addTask(new Task("A", "Task A", PRINT, new PrintTask("A done", 300)));
        wf.addTask(new Task("B", "Task B", Set.of("A"), PRINT, new PrintTask("B done")));
        wf.addTask(new Task("C", "Task C", Set.of("B"), PRINT, new PrintTask("C done")));

        // Hold reference to the instance so canceller can use it
        final WorkflowInstance[] wfiRef = new WorkflowInstance[1];

        Thread canceller = new Thread(() -> {
            sleep(100);
            System.out.println("[Test] Requesting cancellation...");
            // Cancel by workflowId — engine finds active instance
            engine.cancelWorkflow(wf.getId());
        });
        canceller.start();

        WorkflowInstance wfi = engine.submit(wf);
        wfiRef[0] = wfi;
        canceller.join();

        assert statusOf(wfi, "A") == TaskStatus.SUCCESS : "A should be SUCCESS";
        assert statusOf(wfi, "B") == TaskStatus.CANCELLED : "B should be CANCELLED";
        assert statusOf(wfi, "C") == TaskStatus.CANCELLED : "C should be CANCELLED";
        assert wfi.getStatus() == WorkflowStatus.CANCELLED : "Workflow should be CANCELLED";
        System.out.println("✓ Test 7 passed");
    }


    // ── Test 8: B fails once then succeeds on retry ───────────────────────────

    static void testRetry(WorkflowEngine engine) throws Exception {
        System.out.println("\n══════════════════════════════════════");
        System.out.println(" TEST 8 — Retry: A -> B(fails once, retries) -> C");
        System.out.println("══════════════════════════════════════");

        Workflow wf = new Workflow("wf-retry");
        wf.addTask(new Task("A", "Task A", PRINT, new PrintTask("A done", 50)));
        wf.addTask(new Task("B", "Task B", Set.of("A"),
                FAIL, new FailOnceThenSucceedTask("B"),
                new RetryPolicy(3, 100)));  // 3 attempts, 100ms backoff
        wf.addTask(new Task("C", "Task C", Set.of("B"), PRINT, new PrintTask("C done")));

        WorkflowInstance wfi = engine.submit(wf);

        assert statusOf(wfi, "A") == TaskStatus.SUCCESS : "A should be SUCCESS";
        assert statusOf(wfi, "B") == TaskStatus.SUCCESS : "B should eventually SUCCESS";
        assert statusOf(wfi, "C") == TaskStatus.SUCCESS : "C should be SUCCESS";

        // B should have 2 logs — 1 failed attempt + 1 success
        List<TaskExecutionLog> bLogs =
                engine.getLoggingService().getLogsForTask(
                        wfi.findByTaskId("B").getInstanceId());
        assert bLogs.size() == 2 : "B should have 2 attempt logs, got " + bLogs.size();
        assert bLogs.get(0).getStatus() == TaskStatus.FAILED : "B attempt 1 should be FAILED";
        assert bLogs.get(1).getStatus() == TaskStatus.SUCCESS : "B attempt 2 should be SUCCESS";
        System.out.println("  B attempt logs: " + bLogs);
        System.out.println("✓ Test 8 passed — retry confirmed");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    static TaskStatus statusOf(WorkflowInstance wfi, String taskId) {
        return wfi.findByTaskId(taskId).getStatus();
    }
}