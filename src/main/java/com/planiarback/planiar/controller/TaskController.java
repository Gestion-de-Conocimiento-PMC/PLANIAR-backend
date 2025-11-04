package com.planiarback.planiar.controller;

import com.planiarback.planiar.model.Task;
import com.planiarback.planiar.service.TaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;
    private static final Logger logger = LoggerFactory.getLogger(TaskController.class);

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    /**
     * Crear una nueva tarea
     * POST /api/tasks/user/{userId}
     */
    @PostMapping("/user/{userId}")
    public ResponseEntity<?> createTask(@PathVariable Long userId, @RequestBody Task task) {
        try {
            Task createdTask = taskService.createTask(task, userId);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdTask);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            // Log full stacktrace for debugging and return a controlled 500 response
            logger.error("Unexpected error while creating task for user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error", "details", e.getMessage()));
        }
    }

    /**
     * Obtener todas las tareas
     * GET /api/tasks
     */
    @GetMapping
    public ResponseEntity<List<Task>> getAllTasks() {
        List<Task> tasks = taskService.getAllTasks();
        return ResponseEntity.ok(tasks);
    }

    /**
     * Obtener todas las tareas de un usuario
     * GET /api/tasks/user/{userId}
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Task>> getAllTasksByUser(@PathVariable Long userId) {
        List<Task> tasks = taskService.getAllTasksByUser(userId);
        return ResponseEntity.ok(tasks);
    }

    /**
     * Obtener tareas ordenadas por fecha
     * GET /api/tasks/user/{userId}/ordered-by-date
     */
    @GetMapping("/user/{userId}/ordered-by-date")
    public ResponseEntity<List<Task>> getTasksOrderedByDate(@PathVariable Long userId) {
        List<Task> tasks = taskService.getTasksOrderedByDueDate(userId);
        return ResponseEntity.ok(tasks);
    }

    /**
     * Obtener tareas ordenadas por prioridad
     * GET /api/tasks/user/{userId}/ordered-by-priority
     */
    @GetMapping("/user/{userId}/ordered-by-priority")
    public ResponseEntity<List<Task>> getTasksOrderedByPriority(@PathVariable Long userId) {
        List<Task> tasks = taskService.getTasksOrderedByPriority(userId);
        return ResponseEntity.ok(tasks);
    }

    /**
     * Obtener una tarea por ID
     * GET /api/tasks/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getTaskById(@PathVariable Long id) {
        return taskService.getTaskById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Obtener tarea por título
     * GET /api/tasks/user/{userId}/title/{title}
     */
    @GetMapping("/user/{userId}/title/{title}")
    public ResponseEntity<?> getTaskByTitle(@PathVariable Long userId, @PathVariable String title) {
        return taskService.getTaskByTitle(userId, title)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Buscar tareas por título
     * GET /api/tasks/user/{userId}/search?title=tarea
     */
    @GetMapping("/user/{userId}/search")
    public ResponseEntity<List<Task>> searchTasksByTitle(
            @PathVariable Long userId,
            @RequestParam String title) {
        List<Task> tasks = taskService.searchTasksByTitle(userId, title);
        return ResponseEntity.ok(tasks);
    }

    /**
     * Obtener tareas de una clase específica
     * GET /api/tasks/user/{userId}/class/{classId}
     */
    @GetMapping("/user/{userId}/class/{classId}")
    public ResponseEntity<List<Task>> getTasksByClass(
            @PathVariable Long userId,
            @PathVariable Long classId) {
        List<Task> tasks = taskService.getTasksByClass(userId, classId);
        return ResponseEntity.ok(tasks);
    }

    /**
     * Obtener tareas por estado
     * GET /api/tasks/user/{userId}/state/{state}
     */
    @GetMapping("/user/{userId}/state/{state}")
    public ResponseEntity<List<Task>> getTasksByState(
            @PathVariable Long userId,
            @PathVariable String state) {
        List<Task> tasks = taskService.getTasksByState(userId, state);
        return ResponseEntity.ok(tasks);
    }

    /**
     * Obtener tareas por prioridad
     * GET /api/tasks/user/{userId}/priority/{priority}
     */
    @GetMapping("/user/{userId}/priority/{priority}")
    public ResponseEntity<List<Task>> getTasksByPriority(
            @PathVariable Long userId,
            @PathVariable String priority) {
        List<Task> tasks = taskService.getTasksByPriority(userId, priority);
        return ResponseEntity.ok(tasks);
    }

    /**
     * Obtener tareas por tipo
     * GET /api/tasks/user/{userId}/type/{type}
     */
    @GetMapping("/user/{userId}/type/{type}")
    public ResponseEntity<List<Task>> getTasksByType(
            @PathVariable Long userId,
            @PathVariable String type) {
        List<Task> tasks = taskService.getTasksByType(userId, type);
        return ResponseEntity.ok(tasks);
    }

    /**
     * Obtener tareas de una fecha específica
     * GET /api/tasks/user/{userId}/due-date/{dueDate}
     */
    @GetMapping("/user/{userId}/due-date/{dueDate}")
    public ResponseEntity<List<Task>> getTasksByDueDate(
            @PathVariable Long userId,
            @PathVariable String dueDate) {
        LocalDate parsedDate = LocalDate.parse(dueDate);
        List<Task> tasks = taskService.getTasksByDueDate(userId, parsedDate);
        return ResponseEntity.ok(tasks);
    }

    /**
     * Obtener tareas en un rango de fechas
     * GET /api/tasks/user/{userId}/range?startDate=2024-03-01&endDate=2024-03-31
     */
    @GetMapping("/user/{userId}/range")
    public ResponseEntity<List<Task>> getTasksInRange(
            @PathVariable Long userId,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);
        List<Task> tasks = taskService.getTasksInRange(userId, start, end);
        return ResponseEntity.ok(tasks);
    }

    /**
     * Obtener tareas vencidas
     * GET /api/tasks/user/{userId}/overdue
     */
    @GetMapping("/user/{userId}/overdue")
    public ResponseEntity<List<Task>> getOverdueTasks(@PathVariable Long userId) {
        List<Task> tasks = taskService.getOverdueTasks(userId);
        return ResponseEntity.ok(tasks);
    }

    /**
     * Obtener tareas futuras
     * GET /api/tasks/user/{userId}/future
     */
    @GetMapping("/user/{userId}/future")
    public ResponseEntity<List<Task>> getFutureTasks(@PathVariable Long userId) {
        List<Task> tasks = taskService.getFutureTasks(userId);
        return ResponseEntity.ok(tasks);
    }

    /**
     * Obtener tareas sin clase asignada
     * GET /api/tasks/user/{userId}/without-class
     */
    @GetMapping("/user/{userId}/without-class")
    public ResponseEntity<List<Task>> getTasksWithoutClass(@PathVariable Long userId) {
        List<Task> tasks = taskService.getTasksWithoutClass(userId);
        return ResponseEntity.ok(tasks);
    }

    /**
     * Obtener tareas con clase asignada
     * GET /api/tasks/user/{userId}/with-class
     */
    @GetMapping("/user/{userId}/with-class")
    public ResponseEntity<List<Task>> getTasksWithClass(@PathVariable Long userId) {
        List<Task> tasks = taskService.getTasksWithClass(userId);
        return ResponseEntity.ok(tasks);
    }

    /**
     * Obtener tareas sin fecha
     * GET /api/tasks/user/{userId}/without-date
     */
    @GetMapping("/user/{userId}/without-due-date")
    public ResponseEntity<List<Task>> getTasksWithoutDueDate(@PathVariable Long userId) {
        List<Task> tasks = taskService.getTasksWithoutDueDate(userId);
        return ResponseEntity.ok(tasks);
    }

    /**
     * Obtener tareas sin descripción
     * GET /api/tasks/user/{userId}/without-description
     */
    @GetMapping("/user/{userId}/without-description")
    public ResponseEntity<List<Task>> getTasksWithoutDescription(@PathVariable Long userId) {
        List<Task> tasks = taskService.getTasksWithoutDescription(userId);
        return ResponseEntity.ok(tasks);
    }

    /**
     * Actualizar una tarea
     * PUT /api/tasks/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateTask(@PathVariable Long id, @RequestBody Task taskDetails) {
        try {
            Task updatedTask = taskService.updateTask(id, taskDetails);
            return ResponseEntity.ok(updatedTask);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Eliminar una tarea
     * DELETE /api/tasks/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTask(@PathVariable Long id) {
        try {
            taskService.deleteTask(id);
            return ResponseEntity.ok(Map.of("message", "Tarea eliminada exitosamente"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Eliminar todas las tareas de un usuario
     * DELETE /api/tasks/user/{userId}/all
     */
    @DeleteMapping("/user/{userId}/all")
    public ResponseEntity<?> deleteAllUserTasks(@PathVariable Long userId) {
        taskService.deleteAllUserTasks(userId);
        return ResponseEntity.ok(Map.of("message", "Todas las tareas eliminadas exitosamente"));
    }

    /**
     * Contar tareas de un usuario
     * GET /api/tasks/user/{userId}/count
     */
    @GetMapping("/user/{userId}/count")
    public ResponseEntity<Map<String, Long>> countUserTasks(@PathVariable Long userId) {
        long count = taskService.countUserTasks(userId);
        return ResponseEntity.ok(Map.of("count", count));
    }

    /**
     * Contar tareas de un usuario por estado
     * GET /api/tasks/user/{userId}/count-by-state/{state}
     */
    @GetMapping("/user/{userId}/count-by-state/{state}")
    public ResponseEntity<Map<String, Long>> countTasksByState(
            @PathVariable Long userId,
            @PathVariable String state) {
        long count = taskService.countTasksByState(userId, state);
        return ResponseEntity.ok(Map.of("count", count));
    }

    /**
     * Contar tareas de un usuario por prioridad
     * GET /api/tasks/user/{userId}/count-by-priority/{priority}
     */
    @GetMapping("/user/{userId}/count-by-priority/{priority}")
    public ResponseEntity<Map<String, Long>> countTasksByPriority(
            @PathVariable Long userId,
            @PathVariable String priority) {
        long count = taskService.countTasksByPriority(userId, priority);
        return ResponseEntity.ok(Map.of("count", count));
    }

    /**
     * Verificar si existe una tarea con ese título
     * GET /api/tasks/user/{userId}/exists?title=tarea
     */
    @GetMapping("/user/{userId}/exists")
    public ResponseEntity<Map<String, Boolean>> checkTaskExists(
            @PathVariable Long userId,
            @RequestParam String title) {
        boolean exists = taskService.existsByUserIdAndTitle(userId, title);
        return ResponseEntity.ok(Map.of("exists", exists));
    }

    /**
     * Asignar tarea a una clase
     * PATCH /api/tasks/{taskId}/assign-class/{classId}
     */
    @PatchMapping("/{taskId}/assign-class/{classId}")
    public ResponseEntity<?> assignTaskToClass(
            @PathVariable Long taskId,
            @PathVariable Long classId) {
        try {
            Task task = taskService.assignTaskToClass(taskId, classId);
            return ResponseEntity.ok(task);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Remover tarea de una clase
     * PATCH /api/tasks/{taskId}/remove-class
     */
    @PatchMapping("/{taskId}/remove-class")
    public ResponseEntity<?> removeTaskFromClass(@PathVariable Long taskId) {
        try {
            Task task = taskService.removeTaskFromClass(taskId);
            return ResponseEntity.ok(task);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Cambiar estado de una tarea
     * PATCH /api/tasks/{taskId}/state/{newState}
     */
    @PatchMapping("/{taskId}/state/{newState}")
    public ResponseEntity<?> updateTaskState(
            @PathVariable Long taskId,
            @PathVariable String newState) {
        try {
            Task task = taskService.updateTaskState(taskId, newState);
            return ResponseEntity.ok(task);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }
}