package com.workflow.workflow.core;

import com.workflow.workflow.core.tasks.TaskFunction;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TaskRegistry
 * <p>
 * Maps a stored className (String) → live Callable the engine invokes.
 * <p>
 * The className is persisted in Task.className (a plain DB column).
 * The Callable is resolved fresh at execution time — never stored.
 * <p>
 * Lifecycle:
 * 1. Startup  — register(SendEmailTask.class)
 * 2. Execution — engine calls resolve("com.workflow.tasks.SendEmailTask")
 * → new SendEmailTask().execute()
 * <p>
 * Thread safety:
 * ConcurrentHashMap — register() and resolve() are safe to call concurrently.
 * resolve() creates a new task instance per call — tasks must be stateless.
 */
public class TaskRegistry {

    private final Map<String, Class<? extends TaskFunction>> store = new ConcurrentHashMap<>();

    // ── Registration ──────────────────────────────────────────────────────────

    /**
     * Register a JavaTask class.
     * Validates the public no-arg constructor at registration time —
     * fails fast on startup rather than mid-execution.
     */
    public void register(Class<? extends TaskFunction> taskClass) {
        validateConstructor(taskClass);
        store.put(taskClass.getName(), taskClass);
        System.out.println("[TaskRegistry] Registered: " + taskClass.getName());
    }

    // ── Resolution ────────────────────────────────────────────────────────────

    /**
     * Resolve a className to a fresh Callable.
     * Returns a new task instance each time — tasks must be stateless.
     * <p>
     * Skips resolution for "inline" className — task already has
     * executionFn set directly (used in tests with lambda constructors).
     *
     * @throws IllegalArgumentException if className was never registered
     */
    public Callable<Void> resolve(String className) {
        if ("inline".equals(className)) {
            throw new IllegalArgumentException(
                    "Cannot resolve 'inline' tasks via registry — executionFn is set directly");
        }

        Class<? extends TaskFunction> taskClass = store.get(className);
        if (taskClass == null) {
            throw new IllegalArgumentException(
                    "No task registered for: '" + className + "'. "
                            + "Call registry.register(" + simpleClassName(className) + ".class) at startup.");
        }

        // Fresh instance per call — stateless by contract
        return () -> {
            taskClass.getDeclaredConstructor().newInstance().execute();
            return null;
        };
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    public boolean isRegistered(String className) {
        return store.containsKey(className);
    }

    public Set<String> registeredClassNames() {
        return Collections.unmodifiableSet(store.keySet());
    }

    // ── Private ───────────────────────────────────────────────────────────────

    private void validateConstructor(Class<? extends TaskFunction> taskClass) {
        try {
            taskClass.getDeclaredConstructor();
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException(
                    taskClass.getName() + " must have a public no-arg constructor", e);
        }
    }

    private String simpleClassName(String fullyQualified) {
        int dot = fullyQualified.lastIndexOf('.');
        return dot >= 0 ? fullyQualified.substring(dot + 1) : fullyQualified;
    }
}