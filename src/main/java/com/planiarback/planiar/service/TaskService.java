package com.planiarback.planiar.service;

import com.planiarback.planiar.model.Task;
import com.planiarback.planiar.model.User;
import com.planiarback.planiar.repository.TaskRepository;
import com.planiarback.planiar.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final UserService userService;

    public TaskService(TaskRepository taskRepository, UserRepository userRepository, UserService userService) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.userService = userService;
    }

    /**
     * Crear una nueva tarea
     */
    public Task createTask(Task task, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con id: " + userId));
        
        task.setUser(user);
        validateTask(task);
        Task saved = taskRepository.save(task);
        // Recalculate user's available hours
        userService.recalculateAvailableHours(user);
        return saved;
    }

    /**
     * Obtener todas las tareas
     */
    @Transactional(readOnly = true)
    public List<Task> getAllTasks() {
        return taskRepository.findAll();
    }

    /**
     * Obtener todas las tareas de un usuario
     */
    @Transactional(readOnly = true)
    public List<Task> getAllTasksByUser(Long userId) {
        return taskRepository.findByUserId(userId);
    }

    /**
     * Obtener tareas ordenadas por fecha
     */
    @Transactional(readOnly = true)
    public List<Task> getTasksOrderedByDueDate(Long userId) {
        return taskRepository.findByUserIdOrderByDueDateAsc(userId);
    }

    /**
     * Obtener tareas ordenadas por prioridad
     */
    @Transactional(readOnly = true)
    public List<Task> getTasksOrderedByPriority(Long userId) {
        return taskRepository.findByUserIdOrderByPriorityAsc(userId);
    }

    /**
     * Obtener una tarea por ID
     */
    @Transactional(readOnly = true)
    public Optional<Task> getTaskById(Long id) {
        return taskRepository.findById(id);
    }

    /**
     * Obtener tarea por título de un usuario
     */
    @Transactional(readOnly = true)
    public Optional<Task> getTaskByTitle(Long userId, String title) {
        return taskRepository.findByUserIdAndTitle(userId, title);
    }

    /**
     * Buscar tareas por título
     */
    @Transactional(readOnly = true)
    public List<Task> searchTasksByTitle(Long userId, String title) {
        return taskRepository.findByUserIdAndTitleContainingIgnoreCase(userId, title);
    }

    /**
     * Obtener tareas de una clase específica
     */
    @Transactional(readOnly = true)
    public List<Task> getTasksByClass(Long userId, Long classId) {
        return taskRepository.findByUserIdAndClassId(userId, classId);
    }

    /**
     * Obtener tareas por estado
     */
    @Transactional(readOnly = true)
    public List<Task> getTasksByState(Long userId, String state) {
        return taskRepository.findByUserIdAndState(userId, state);
    }

    /**
     * Obtener tareas por prioridad
     */
    @Transactional(readOnly = true)
    public List<Task> getTasksByPriority(Long userId, String priority) {
        return taskRepository.findByUserIdAndPriority(userId, priority);
    }

    /**
     * Obtener tareas por tipo
     */
    @Transactional(readOnly = true)
    public List<Task> getTasksByType(Long userId, String type) {
        return taskRepository.findByUserIdAndType(userId, type);
    }

    /**
     * Obtener tareas de una fecha específica
     */
    @Transactional(readOnly = true)
    public List<Task> getTasksByDueDate(Long userId, LocalDate dueDate) {
        return taskRepository.findByUserIdAndDueDate(userId, dueDate);
    }

    /**
     * Obtener tareas en un rango de fechas
     */
    @Transactional(readOnly = true)
    public List<Task> getTasksInRange(Long userId, LocalDate startDate, LocalDate endDate) {
        return taskRepository.findByUserIdAndDueDateBetween(userId, startDate, endDate);
    }

    /**
     * Obtener tareas vencidas
     */
    @Transactional(readOnly = true)
    public List<Task> getOverdueTasks(Long userId) {
        return taskRepository.findByUserIdAndDueDateBefore(userId, LocalDate.now());
    }

    /**
     * Obtener tareas futuras
     */
    @Transactional(readOnly = true)
    public List<Task> getFutureTasks(Long userId) {
        return taskRepository.findByUserIdAndDueDateAfterOrderByDueDateAsc(userId, LocalDate.now());
    }

    /**
     * Obtener tareas sin clase asignada
     */
    @Transactional(readOnly = true)
    public List<Task> getTasksWithoutClass(Long userId) {
        return taskRepository.findByUserIdAndClassIdIsNull(userId);
    }

    /**
     * Obtener tareas con clase asignada
     */
    @Transactional(readOnly = true)
    public List<Task> getTasksWithClass(Long userId) {
        return taskRepository.findByUserIdAndClassIdIsNotNull(userId);
    }

    /**
     * Obtener tareas sin dueDate
     */
    @Transactional(readOnly = true)
    public List<Task> getTasksWithoutDueDate(Long userId) {
        return taskRepository.findByUserIdAndDueDateIsNull(userId);
    }

    /**
     * Obtener tareas sin descripción
     */
    @Transactional(readOnly = true)
    public List<Task> getTasksWithoutDescription(Long userId) {
        return taskRepository.findByUserIdAndDescriptionIsNull(userId);
    }

    /**
     * Actualizar una tarea
     */
    public Task updateTask(Long id, Task taskDetails) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tarea no encontrada con id: " + id));

        Long userId = task.getUser().getId();

        // Verificar si el nuevo título ya existe (y no es la misma tarea)
        if (!task.getTitle().equals(taskDetails.getTitle()) &&
            taskRepository.existsByUserIdAndTitle(userId, taskDetails.getTitle())) {
            throw new IllegalArgumentException("Ya existe una tarea con este título");
        }

        task.setTitle(taskDetails.getTitle());
        task.setClassId(taskDetails.getClassId());
        task.setDueDate(taskDetails.getDueDate());
        task.setWorkingDate(taskDetails.getWorkingDate());
        task.setStartTime(taskDetails.getStartTime());
        task.setEndTime(taskDetails.getEndTime());
        task.setPriority(taskDetails.getPriority());
        task.setEstimatedTime(taskDetails.getEstimatedTime());
        task.setDescription(taskDetails.getDescription());
        task.setType(taskDetails.getType());
        task.setState(taskDetails.getState());

        validateTask(task);
        Task saved = taskRepository.save(task);
        userService.recalculateAvailableHours(task.getUser());
        return saved;
    }

    /**
     * Eliminar una tarea
     */
    public void deleteTask(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tarea no encontrada con id: " + id));
        User user = task.getUser();
        taskRepository.deleteById(id);
        if (user != null) userService.recalculateAvailableHours(user);
    }

    /**
     * Eliminar todas las tareas de un usuario
     */
    public void deleteAllUserTasks(Long userId) {
        taskRepository.deleteByUserId(userId);
        userRepository.findById(userId).ifPresent(userService::recalculateAvailableHours);
    }

    /**
     * Contar tareas de un usuario
     */
    @Transactional(readOnly = true)
    public long countUserTasks(Long userId) {
        return taskRepository.countByUserId(userId);
    }

    /**
     * Contar tareas de un usuario por estado
     */
    @Transactional(readOnly = true)
    public long countTasksByState(Long userId, String state) {
        return taskRepository.countByUserIdAndState(userId, state);
    }

    /**
     * Contar tareas de un usuario por prioridad
     */
    @Transactional(readOnly = true)
    public long countTasksByPriority(Long userId, String priority) {
        return taskRepository.countByUserIdAndPriority(userId, priority);
    }

    /**
     * Verificar si existe una tarea con ese título
     */
    @Transactional(readOnly = true)
    public boolean existsByUserIdAndTitle(Long userId, String title) {
        return taskRepository.existsByUserIdAndTitle(userId, title);
    }

    /**
     * Asignar tarea a una clase
     */
    public Task assignTaskToClass(Long taskId, Long classId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Tarea no encontrada con id: " + taskId));

        task.setClassId(classId);
        Task saved = taskRepository.save(task);
        userService.recalculateAvailableHours(task.getUser());
        return saved;
    }

    /**
     * Remover tarea de una clase
     */
    public Task removeTaskFromClass(Long taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Tarea no encontrada con id: " + taskId));

        task.setClassId(null);
        Task saved = taskRepository.save(task);
        userService.recalculateAvailableHours(task.getUser());
        return saved;
    }

    /**
     * Cambiar estado de una tarea
     */
    public Task updateTaskState(Long taskId, String newState) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Tarea no encontrada con id: " + taskId));

        task.setState(newState);
        Task saved = taskRepository.save(task);
        userService.recalculateAvailableHours(task.getUser());
        return saved;
    }

    /**
     * Validar datos de la tarea
     */
    private void validateTask(Task task) {
        if (task.getTitle() == null || task.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("El título no puede estar vacío");
        }

        if (task.getPriority() != null && !task.getPriority().matches("^(High|Medium|Low)$")) {
            throw new IllegalArgumentException("Prioridad inválida. Use: High, Medium, Low");
        }

        if (task.getType() != null && task.getType().trim().isEmpty()) {
            throw new IllegalArgumentException("El tipo no puede estar vacío si se proporciona");
        }

        if (task.getEstimatedTime() <= 0) {
            throw new IllegalArgumentException("El tiempo estimado debe ser mayor a 0");
        }
    }
}