package com.workflow.workflow;

import com.workflow.workflow.core.*;
import com.workflow.workflow.core.tasks.FailingTask;
import com.workflow.workflow.core.tasks.PrintTask;
import com.workflow.workflow.database.TaskRepository;
import com.workflow.workflow.database.WorkflowRepository;
import com.workflow.workflow.database.WorkflowService;
import com.workflow.workflow.database.impl.InMemoryTaskRepository;
import com.workflow.workflow.database.impl.InMemoryWorkflowRepository;
import com.workflow.workflow.database.impl.WorkflowServiceImpl;
import com.workflow.workflow.entities.task.TaskStatus;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.nio.file.Path;
import java.util.Set;

@SpringBootApplication
public class WorkflowApplication {

    public static void main(String[] args) {
        SpringApplication.run(WorkflowApplication.class, args);
    }

    @Bean
    public CommandLineRunner commandLineRunner() {
        return args -> {
            TaskRepository taskRepository = new InMemoryTaskRepository();
            WorkflowRepository workflowRepository = new InMemoryWorkflowRepository(taskRepository);
            WorkflowService workflowService = new WorkflowServiceImpl(taskRepository, workflowRepository);
            TaskRegistry taskRegistry = new TaskRegistry();
            WorkflowEngine engine = new WorkflowEngine(workflowService, taskRegistry);

            testLinear(engine);
            testParallel(engine);
            testCycleRejected(engine);
            testMissingDep(engine);
            testFailureCascade(engine);
            testDeepCascade(engine);
            testCancel(engine);

            engine.shutdown();
            System.out.println("\n🎉 All tests passed.");
        };
    }

    // ── Test 1: A → B → C (sequential) ───────────────────────────────────────

    static void testLinear(WorkflowEngine engine) throws Exception {
        System.out.println("\n══════════════════════════════════════");
        System.out.println(" TEST 1 — Linear: A -> B -> C");
        System.out.println("══════════════════════════════════════");

        Workflow wf = new Workflow("wf-linear");
        wf.addTask(new Task("A", "Task A", "com.workflow.workflow.tasks.PrintTask", new PrintTask("A done", 100)));
        wf.addTask(new Task("B", "Task B", Set.of("A"), "com.workflow.workflow.tasks.PrintTask", new PrintTask("B done", 100)));
        wf.addTask(new Task("C", "Task C", Set.of("B"), "com.workflow.workflow.tasks.PrintTask", new PrintTask("C done", 100)));

        engine.submit(wf);

        assert statusOf(wf, "A") == TaskStatus.SUCCESS;
        assert statusOf(wf, "B") == TaskStatus.SUCCESS;
        assert statusOf(wf, "C") == TaskStatus.SUCCESS;
        System.out.println("✓ Test 1 passed");
    }

    // ── Test 2: A + B independent → C (parallel) ─────────────────────────────

    static void testParallel(WorkflowEngine engine) throws Exception {
        System.out.println("\n══════════════════════════════════════");
        System.out.println(" TEST 2 — Parallel: A + B -> C");
        System.out.println("══════════════════════════════════════");

        Workflow wf = new Workflow("wf-parallel");
        wf.addTask(new Task("A", "Task A", "com.workflow.workflow.tasks.PrintTask", new PrintTask("A done", 200)));
        wf.addTask(new Task("B", "Task B", "com.workflow.workflow.tasks.PrintTask", new PrintTask("B done", 200)));
        wf.addTask(new Task("C", "Task C", Set.of("A", "B"), "com.workflow.workflow.tasks.PrintTask", new PrintTask("C done", 100)));

        WorkflowVisualizer.export(wf, Path.of("workflow-plan.dot"));
        long start = System.currentTimeMillis();
        engine.submit(wf);
        long elapsed = System.currentTimeMillis() - start;
        WorkflowVisualizer.export(wf, Path.of("workflow-result.dot"));

        System.out.println("[Test] Wall time: " + elapsed + "ms (sequential ~500ms, parallel ~300ms)");
        assert elapsed < 450 : "Expected parallel execution, got " + elapsed + "ms";
        assert statusOf(wf, "A") == TaskStatus.SUCCESS;
        assert statusOf(wf, "B") == TaskStatus.SUCCESS;
        assert statusOf(wf, "C") == TaskStatus.SUCCESS;
        System.out.println("✓ Test 2 passed — parallelism confirmed");
    }

    // ── Test 3: Cycle rejected ────────────────────────────────────────────────

    static void testCycleRejected(WorkflowEngine engine) {
        System.out.println("\n══════════════════════════════════════");
        System.out.println(" TEST 3 — Cycle: A -> B -> A");
        System.out.println("══════════════════════════════════════");

        Workflow wf = new Workflow("wf-cycle");
        wf.addTask(new Task("A", "Task A", Set.of("B"), "com.workflow.workflow.tasks.PrintTask", new PrintTask("A done")));
        wf.addTask(new Task("B", "Task B", Set.of("A"), "com.workflow.workflow.tasks.PrintTask", new PrintTask("B done")));

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

    // ── Test 4: Unknown dependency rejected ───────────────────────────────────

    static void testMissingDep(WorkflowEngine engine) {
        System.out.println("\n══════════════════════════════════════");
        System.out.println(" TEST 4 — Unknown dep: B -> X");
        System.out.println("══════════════════════════════════════");

        Workflow wf = new Workflow("wf-missing");
        wf.addTask(new Task("A", "Task A", "com.workflow.workflow.tasks.PrintTask", new PrintTask("A done")));
        wf.addTask(new Task("B", "Task B", Set.of("X"), "com.workflow.workflow.tasks.PrintTask", new PrintTask("B done")));

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

    // ── Test 5: B fails → C skipped ──────────────────────────────────────────

    static void testFailureCascade(WorkflowEngine engine) throws Exception {
        System.out.println("\n══════════════════════════════════════");
        System.out.println(" TEST 5 — Cascade: A -> B(fail) -> C(skip)");
        System.out.println("══════════════════════════════════════");

        Workflow wf = new Workflow("wf-cascade");
        wf.addTask(new Task("A", "Task A", "com.workflow.workflow.tasks.PrintTask", new PrintTask("A done", 50)));
        wf.addTask(new Task("B", "Task B", Set.of("A"), "com.workflow.workflow.tasks.FailingTask", new FailingTask("B exploded")));
        wf.addTask(new Task("C", "Task C", Set.of("B"), "com.workflow.workflow.tasks.PrintTask", new PrintTask("C done")));

        engine.submit(wf);

        assert statusOf(wf, "A") == TaskStatus.SUCCESS : "A should be SUCCESS";
        assert statusOf(wf, "B") == TaskStatus.FAILED : "B should be FAILED";
        assert statusOf(wf, "C") == TaskStatus.SKIPPED : "C should be SKIPPED";
        System.out.println("✓ Test 5 passed");
    }

    // ── Test 6: Deep cascade B fails → C → D all skipped ─────────────────────

    static void testDeepCascade(WorkflowEngine engine) throws Exception {
        System.out.println("\n══════════════════════════════════════");
        System.out.println(" TEST 6 — Deep cascade: A -> B(fail) -> C -> D");
        System.out.println("══════════════════════════════════════");

        Workflow wf = new Workflow("wf-deep");
        wf.addTask(new Task("A", "Task A", "com.workflow.workflow.tasks.PrintTask", new PrintTask("A done", 50)));
        wf.addTask(new Task("B", "Task B", Set.of("A"), "com.workflow.workflow.tasks.FailingTask", new FailingTask("B exploded")));
        wf.addTask(new Task("C", "Task C", Set.of("B"), "com.workflow.workflow.tasks.PrintTask", new PrintTask("C done")));
        wf.addTask(new Task("D", "Task D", Set.of("C"), "com.workflow.workflow.tasks.PrintTask", new PrintTask("D done")));

        engine.submit(wf);

        assert statusOf(wf, "A") == TaskStatus.SUCCESS : "A should be SUCCESS";
        assert statusOf(wf, "B") == TaskStatus.FAILED : "B should be FAILED";
        assert statusOf(wf, "C") == TaskStatus.SKIPPED : "C should be SKIPPED";
        assert statusOf(wf, "D") == TaskStatus.SKIPPED : "D should be SKIPPED";
        System.out.println("✓ Test 6 passed");
    }

    // ── Test 7: Cancel mid-execution ──────────────────────────────────────────

    static void testCancel(WorkflowEngine engine) throws Exception {
        System.out.println("\n══════════════════════════════════════");
        System.out.println(" TEST 7 — Cancel: A runs, B and C cancelled");
        System.out.println("══════════════════════════════════════");

        Workflow wf = new Workflow("wf-cancel");
        wf.addTask(new Task("A", "Task A", "com.workflow.workflow.tasks.PrintTask", new PrintTask("A done", 300)));
        wf.addTask(new Task("B", "Task B", Set.of("A"), "com.workflow.workflow.tasks.PrintTask", new PrintTask("B done")));
        wf.addTask(new Task("C", "Task C", Set.of("B"), "com.workflow.workflow.tasks.PrintTask", new PrintTask("C done")));

        Thread canceller = new Thread(() -> {
            sleep(100);
            System.out.println("[Test] Requesting cancellation...");
            engine.cancelWorkflow("wf-cancel");
        });
        canceller.start();

        engine.submit(wf);
        canceller.join();

        assert statusOf(wf, "A") == TaskStatus.SUCCESS : "A should be SUCCESS";
        assert statusOf(wf, "B") == TaskStatus.CANCELLED : "B should be CANCELLED";
        assert statusOf(wf, "C") == TaskStatus.CANCELLED : "C should be CANCELLED";
        assert wf.getStatus() == WorkflowStatus.CANCELLED : "Workflow should be CANCELLED";
        System.out.println("✓ Test 7 passed");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    static TaskStatus statusOf(Workflow wf, String id) {
        return wf.getTasks().stream()
                .filter(t -> t.getId().equals(id))
                .findFirst().orElseThrow()
                .getStatus();
    }
}