package com.workflow.workflow.core.util;

import com.workflow.workflow.core.model.Task;

import java.util.*;

public class WorkflowHelper {
    public static List<Task> getZeroIndegreeTasks(Set<Task> tasks) {
        Map<String, Task> idTaskMap = buildIndex(tasks);
        Map<String, Integer> inDegree = new LinkedHashMap<>();
        for (Task t : tasks)
            inDegree.put(t.getId(), t.getDependencies().size());

        List<Task> tasksWithZeroIndegree = new ArrayList<>();
        inDegree.entrySet().stream().forEach(entry -> {
            if (inDegree.get(entry.getKey()) == 0) {
                tasksWithZeroIndegree.add(idTaskMap.get(entry.getKey()));
            }
        });
        return tasksWithZeroIndegree;
    }


    public static Map<String, Task> buildIndex(Set<Task> tasks) {
        Map<String, Task> index = new LinkedHashMap<>();
        for (Task t : tasks) index.put(t.getId(), t);
        return index;
    }

    public static void checkNoDuplicateIds(Set<Task> tasks) {
        Set<String> seen = new HashSet<>();
        for (Task t : tasks) {
            if (!seen.add(t.getId()))
                throw new IllegalStateException("Duplicate task ID: '" + t.getId() + "'");
        }
    }

    public static void checkDependenciesExist(Set<Task> tasks, Map<String, Task> idTaskMap) {
        for (Task t : tasks)
            for (String dep : t.getDependencies())
                if (!idTaskMap.containsKey(dep))
                    throw new IllegalStateException(
                            "Task '" + t.getId() + "' depends on unknown task '" + dep + "'");
    }

    public static void checkNoCycles(Map<String, Task> idTaskMap) {
        Map<String, Integer> visited = new HashMap<>();
        for (String id : idTaskMap.keySet()) visited.put(id, 0);

        for (String id : idTaskMap.keySet())
            if (visited.get(id) == 0)
                dfs(id, idTaskMap, visited);
    }

    public static void dfs(String id, Map<String, Task> idTaskMap, Map<String, Integer> visited) {
        visited.put(id, 1);
        for (String dep : idTaskMap.get(id).getDependencies()) {
            if (visited.get(dep) == 1)
                throw new IllegalStateException(
                        "Cycle detected: '" + id + "' → '" + dep + "' creates a cycle");
            if (visited.get(dep) == 0)
                dfs(dep, idTaskMap, visited);
        }
        visited.put(id, 2);
    }

}
