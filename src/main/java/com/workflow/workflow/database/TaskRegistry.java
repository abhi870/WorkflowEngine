package com.workflow.workflow.database;

import com.workflow.workflow.core.tasks.TaskFunction;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;


public class TaskRegistry {

    private final Map<String, Class<? extends TaskFunction>> store = new ConcurrentHashMap<>();



    public void register(Class<? extends TaskFunction> taskClass) {
        validateConstructor(taskClass);
        store.put(taskClass.getName(), taskClass);
        System.out.println("[TaskRegistry] Registered: " + taskClass.getName());
    }



    public Callable<Void> resolve(String className) {

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


    public boolean isRegistered(String className) {
        return store.containsKey(className);
    }

    public Set<String> registeredClassNames() {
        return Collections.unmodifiableSet(store.keySet());
    }


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