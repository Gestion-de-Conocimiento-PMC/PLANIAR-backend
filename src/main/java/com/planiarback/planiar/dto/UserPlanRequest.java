package com.planiarback.planiar.dto;

import com.planiarback.planiar.model.Task;
import java.util.List;
import java.util.Map;

public class UserPlanRequest {
    private Long userId;
    private List<Task> tasks;
    // availableHours map: DAY -> ["HH:MM-HH:MM", ...]
    private Map<String, java.util.List<String>> availableHours;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public List<Task> getTasks() { return tasks; }
    public void setTasks(List<Task> tasks) { this.tasks = tasks; }

    public Map<String, java.util.List<String>> getAvailableHours() { return availableHours; }
    public void setAvailableHours(Map<String, java.util.List<String>> availableHours) { this.availableHours = availableHours; }
}
