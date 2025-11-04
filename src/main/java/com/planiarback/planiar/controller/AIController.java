package com.planiarback.planiar.controller;

import com.planiarback.planiar.dto.UserPlanRequest;
import com.planiarback.planiar.model.Task;
import com.planiarback.planiar.service.AIPlannerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ai")
public class AIController {

    private final AIPlannerService aiPlannerService;

    public AIController(AIPlannerService aiPlannerService) {
        this.aiPlannerService = aiPlannerService;
    }

    @PostMapping("/refresh-plan")
    public ResponseEntity<List<Task>> refreshPlan(@RequestBody UserPlanRequest request) {
        List<Task> planned = aiPlannerService.planTasks(request.getTasks(), request.getAvailableHours());
        return ResponseEntity.ok(planned);
    }
}
