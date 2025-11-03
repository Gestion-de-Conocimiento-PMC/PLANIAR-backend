package com.planiarback.planiar.service;

import com.planiarback.planiar.model.Task;
import com.planiarback.planiar.model.User;
import com.planiarback.planiar.repository.TaskRepository;
import com.planiarback.planiar.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
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
        // Ensure user's availability is up to date
        userService.recalculateAvailableHours(user);

        // If task has no workingDate/startTime/endTime, try to schedule it automatically
        if (task.getWorkingDate() == null && task.getStartTime() == null && task.getEndTime() == null) {
            try {
                boolean scheduled = tryAutoScheduleTask(task, user);
                if (!scheduled) {
                    // fallback: schedule as late as possible before due (respecting working hours 08:00-22:00)
                    if (task.getDueDate() != null) {
                        LocalDate d = task.getDueDate();
                        LocalTime dueT = task.getDueTime() != null ? task.getDueTime() : LocalTime.of(23, 59);
                        long minutes = task.getEstimatedTime();
                        if (minutes > 0) {
                            LocalTime latestEnd = dueT;
                            if (latestEnd.isAfter(LocalTime.of(22,0))) latestEnd = LocalTime.of(22,0);
                            LocalTime candidateStart = latestEnd.minusMinutes(minutes);
                            if (candidateStart.isBefore(LocalTime.of(8,0))) candidateStart = LocalTime.of(8,0);
                            task.setWorkingDate(d);
                            task.setStartTime(candidateStart);
                            task.setEndTime(candidateStart.plusMinutes(minutes));
                        }
                    }
                }
            } catch (Exception ex) {
                // scheduling failed, proceed without schedule
            }
        }

        Task saved = taskRepository.save(task);
        // Recalculate user's available hours after save
        userService.recalculateAvailableHours(user);
        return saved;
    }

    /**
     * Try to automatically schedule a task using user's availableHours and estimatedTime.
     * Returns true if scheduled.
     */
    private boolean tryAutoScheduleTask(Task task, User user) {
        if (task.getEstimatedTime() <= 0) return false;
        if (task.getDueDate() == null) return false;

        Map<String, java.util.List<String>> avail = user.getAvailableHours();
        if (avail == null) return false;

        long needed = task.getEstimatedTime(); // minutes

        LocalDate deadline = task.getDueDate();
        LocalTime deadlineTime = task.getDueTime() != null ? task.getDueTime() : LocalTime.of(23,59);

        LocalDate today = LocalDate.now();
        // search from deadline backwards to today (limit to 30 days back to avoid long loops)
        int maxBack = 30;
        for (int offset = 0; offset <= maxBack; offset++) {
            LocalDate d = deadline.minusDays(offset);
            if (d.isBefore(today)) break;
            String dayKey = dayNameFor(d.getDayOfWeek().getValue() % 7);
            java.util.List<String> freeSlots = avail.get(dayKey);
            if (freeSlots == null || freeSlots.isEmpty()) continue;

            // iterate free slots and try to fit needed minutes within a single slot
            // choose latest possible slot that finishes before deadline
            for (int i = freeSlots.size() - 1; i >= 0; i--) {
                String slot = freeSlots.get(i); // format "HH:00-HH:00"
                String[] parts = slot.split("-");
                if (parts.length != 2) continue;
                LocalTime slotStart = LocalTime.parse(parts[0]);
                LocalTime slotEnd = LocalTime.parse(parts[1]);

                // if this day is the deadline day, ensure end <= deadlineTime
                LocalTime effectiveSlotEnd = slotEnd;
                if (d.equals(deadline) && effectiveSlotEnd.isAfter(deadlineTime)) {
                    effectiveSlotEnd = deadlineTime;
                }

                long slotMinutes = ChronoUnit.MINUTES.between(slotStart, effectiveSlotEnd);
                if (slotMinutes <= 0) continue;

                if (slotMinutes >= needed) {
                    // schedule within this slot as late as possible
                    LocalTime start = effectiveSlotEnd.minusMinutes(needed);
                    if (start.isBefore(slotStart)) start = slotStart;
                    // respect working hours 08:00-22:00
                    if (start.isBefore(LocalTime.of(8,0))) start = LocalTime.of(8,0);
                    LocalTime end = start.plusMinutes(needed);
                    if (end.isAfter(LocalTime.of(22,0))) {
                        // cannot fit in this slot due to working hours
                        continue;
                    }

                    task.setWorkingDate(d);
                    task.setStartTime(start);
                    task.setEndTime(end);
                    return true;
                }
            }
        }

        return false;
    }

    private String dayNameFor(int idx) {
        // idx: 0=Sunday .. 6=Saturday
        switch (idx) {
            case 0: return "SUN";
            case 1: return "MON";
            case 2: return "TUE";
            case 3: return "WED";
            case 4: return "THU";
            case 5: return "FRI";
            default: return "SAT";
        }
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