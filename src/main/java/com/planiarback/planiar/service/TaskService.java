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
                tryAutoScheduleTask(task, user);
            } catch (Exception ex) {
                // scheduling failed, proceed without schedule
            }
        }

    // After attempting scheduling, check if task covers full estimatedTime
    Integer estimatedObj = task.getEstimatedTime();
    int estimated = estimatedObj != null ? estimatedObj : 0;
        int assignedMinutes = 0;
        if (task.getWorkingDate() != null && task.getStartTime() != null && task.getEndTime() != null) {
            assignedMinutes = (int) ChronoUnit.MINUTES.between(task.getStartTime(), task.getEndTime());
            if (assignedMinutes < 0) assignedMinutes = 0;
        }

        if (assignedMinutes >= estimated || estimated == 0) {
            // single task covers estimate or nothing to schedule
            Task saved = taskRepository.save(task);
            userService.recalculateAvailableHours(user);
            return saved;
        }

        // Need to create segmentation for remaining minutes
        int remaining = estimated - assignedMinutes;
        int remainingSlots = (int) Math.ceil(remaining / 30.0);

        // Save parent task first (it may hold the first assigned block)
        Task parent = taskRepository.save(task);

        // Build occupied set from existing tasks (including parent assigned slots)
        java.util.Set<String> occupied = new java.util.HashSet<>();
        List<Task> existing = taskRepository.findByUserId(user.getId());
        for (Task t : existing) {
            if (t.getWorkingDate() == null || t.getStartTime() == null || t.getEndTime() == null) continue;
            LocalDate d = t.getWorkingDate();
            LocalTime cur = t.getStartTime();
            while (cur.isBefore(t.getEndTime())) {
                occupied.add(d.toString() + "#" + cur.toString());
                cur = cur.plusMinutes(30);
            }
        }

        // Remove parent's assigned slots from remaining search if parent has assigned block
        if (parent.getWorkingDate() != null && parent.getStartTime() != null && parent.getEndTime() != null) {
            LocalDate pd = parent.getWorkingDate();
            LocalTime pc = parent.getStartTime();
            while (pc.isBefore(parent.getEndTime())) {
                String k = pd.toString() + "#" + pc.toString();
                if (occupied.contains(k)) occupied.remove(k);
                pc = pc.plusMinutes(30);
            }
        }

        // Collect candidate slots from user availability
        Map<String, java.util.List<String>> avail = user.getAvailableHours();
        java.util.List<Slot> candidates = new java.util.ArrayList<>();
        LocalDate startDate = LocalDate.now().plusDays(1);
        LocalDate deadline = parent.getDueDate();
        LocalTime deadlineTime = parent.getDueTime() != null ? parent.getDueTime() : LocalTime.of(23,59);
        for (LocalDate d = startDate; d != null && !d.isAfter(deadline); d = d.plusDays(1)) {
            String key = dayNameFor(d.getDayOfWeek().getValue() % 7);
            java.util.List<String> free = avail.get(key);
            if (free == null) continue;
            for (String slot : free) {
                String[] parts = slot.split("-");
                if (parts.length != 2) continue;
                try {
                    LocalTime s = LocalTime.parse(parts[0]);
                    LocalTime e = LocalTime.parse(parts[1]);
                    if (d.equals(deadline) && e.isAfter(deadlineTime)) e = deadlineTime;
                    LocalTime cur = s;
                    while (cur.plusMinutes(30).isBefore(e) || cur.plusMinutes(30).equals(e)) {
                        String keySlot = d.toString() + "#" + cur.toString();
                        if (!occupied.contains(keySlot)) candidates.add(new Slot(d, cur, cur.plusMinutes(30)));
                        cur = cur.plusMinutes(30);
                    }
                } catch (Exception ex) { }
            }
        }

        if (candidates.isEmpty()) {
            // nothing we can do, return parent as saved
            userService.recalculateAvailableHours(user);
            return parent;
        }

        // Group contiguous candidate slots into segments until we cover remainingSlots
        List<Block> blocks = findContiguousBlocks(candidates, remainingSlots);
        List<List<Slot>> allocation = new java.util.ArrayList<>();
        int allocated = 0;
        for (Block b : blocks) {
            if (allocated >= remainingSlots) break;
            List<Slot> seq = b.slots;
            int take = Math.min(seq.size(), remainingSlots - allocated);
            allocation.add(seq.subList(0, take));
            allocated += take;
        }

        // If still not enough, take non-contiguous slots
        if (allocated < remainingSlots) {
            for (Slot s : candidates) {
                if (allocated >= remainingSlots) break;
                // skip slots already used in allocation
                boolean used = false;
                for (List<Slot> g : allocation) for (Slot x : g) if (x.date.equals(s.date) && x.start.equals(s.start)) used = true;
                if (used) continue;
                allocation.add(java.util.List.of(s));
                allocated++;
            }
        }

        // Create segments as child tasks
        int totalSegments = allocation.size() + (assignedMinutes>0 ? 1 : 0);
        int segIndex = 1;
        // Update parent meta if it has assigned block
        if (assignedMinutes > 0) {
            parent.setSegmentIndex(segIndex);
            parent.setTotalSegments(totalSegments);
            parent.setDescription((parent.getDescription() == null ? "" : parent.getDescription() + " \n") + "Avance " + segIndex + "/" + totalSegments + " de " + parent.getTitle());
            taskRepository.save(parent);
            segIndex++;
        }

        for (List<Slot> group : allocation) {
            LocalDate d = group.get(0).date;
            LocalTime s = group.get(0).start;
            LocalTime e = group.get(group.size()-1).end;
            Task seg = new Task();
            seg.setTitle(parent.getTitle());
            seg.setClassId(parent.getClassId());
            seg.setDueDate(parent.getDueDate());
            seg.setDueTime(parent.getDueTime());
            seg.setWorkingDate(d);
            seg.setStartTime(s);
            seg.setEndTime(e);
            seg.setPriority(parent.getPriority());
            int minutes = (int) ChronoUnit.MINUTES.between(s, e);
            seg.setEstimatedTime(minutes);
            seg.setDescription("Avance " + segIndex + "/" + totalSegments + " de " + parent.getTitle());
            seg.setType(parent.getType());
            seg.setState(parent.getState());
            seg.setUser(parent.getUser());
            seg.setParentId(parent.getId());
            seg.setSegmentIndex(segIndex);
            seg.setTotalSegments(totalSegments);
            taskRepository.save(seg);
            segIndex++;
        }

        // Recalculate availability and return parent
        userService.recalculateAvailableHours(user);
        return parent;
    }

    /**
     * Try to automatically schedule a task using user's availableHours and estimatedTime.
     * Returns true if scheduled.
     */
    private boolean tryAutoScheduleTask(Task task, User user) {
        // Enhanced scheduler: 30-min slots, segmentation and basic preemption.
        if (task.getEstimatedTime() <= 0) return false;
        if (task.getDueDate() == null) return false;

        Map<String, java.util.List<String>> avail = user.getAvailableHours();
        if (avail == null) return false;

        int neededMinutes = task.getEstimatedTime();
        int neededSlots = (int) Math.ceil(neededMinutes / 30.0); // number of 30-min slots

        LocalDate deadline = task.getDueDate();
        LocalTime deadlineTime = task.getDueTime() != null ? task.getDueTime() : LocalTime.of(23,59);

        LocalDate startDate = LocalDate.now().plusDays(1); // schedule after today
        if (startDate.isAfter(deadline)) return false;

        // Build list of candidate 30-min slots between startDate and deadline (inclusive)
        List<Slot> candidates = new java.util.ArrayList<>();
        for (LocalDate d = startDate; !d.isAfter(deadline); d = d.plusDays(1)) {
            String dayKey = dayNameFor(d.getDayOfWeek().getValue() % 7);
            java.util.List<String> freeSlots = avail.get(dayKey);
            if (freeSlots == null) continue;
            for (String slot : freeSlots) {
                String[] parts = slot.split("-");
                if (parts.length != 2) continue;
                try {
                    LocalTime s = LocalTime.parse(parts[0]);
                    LocalTime e = LocalTime.parse(parts[1]);
                    // trim if on deadline day
                    if (d.equals(deadline) && e.isAfter(deadlineTime)) e = deadlineTime;
                    if (!s.isBefore(e)) continue;
                    // create 30-min subslots for this free slot
                    LocalTime cur = s;
                    while (cur.plusMinutes(30).isBefore(e) || cur.plusMinutes(30).equals(e)) {
                        candidates.add(new Slot(d, cur, cur.plusMinutes(30)));
                        cur = cur.plusMinutes(30);
                    }
                } catch (Exception ex) {
                    // ignore parse
                }
            }
        }

        if (candidates.isEmpty()) return false;

        // Try to allocate contiguous blocks first
        List<Block> blocks = findContiguousBlocks(candidates, neededSlots);
        if (!blocks.isEmpty()) {
            // If a single block covers all needed slots, schedule accordingly
            if (blocks.size() == 1 && blocks.get(0).slots.size() >= neededSlots) {
                Block b = blocks.get(0);
                // choose latest contiguous sequence within block
                List<Slot> seq = b.slots;
                int startIndex = Math.max(0, seq.size() - neededSlots);
                List<Slot> chosen = seq.subList(startIndex, startIndex + neededSlots);
                // set task to span from first.start to last.end on block date
                task.setWorkingDate(chosen.get(0).date);
                task.setStartTime(chosen.get(0).start);
                task.setEndTime(chosen.get(chosen.size()-1).end);
                return true;
            }
            // else, indicate scheduling possible; actual segmentation handled in createTask
            Block first = blocks.get(0);
            task.setWorkingDate(first.slots.get(0).date);
            task.setStartTime(first.slots.get(0).start);
            task.setEndTime(first.slots.get(Math.min(first.slots.size()-1, neededSlots-1)).end);
            return true;
        }

        // Basic preemption: try to move lower-priority tasks to free up slots
        List<Task> existing = taskRepository.findByUserId(user.getId());
        // sort by priority ascending (Low -> High) so we try to move lower priority first
        existing.sort(java.util.Comparator.comparingInt(this::priorityValue));

        // Map occupied slots initially built from existing tasks
        java.util.Set<String> occupied = new java.util.HashSet<>();
        for (Task t : existing) {
            if (t.getWorkingDate() == null || t.getStartTime() == null || t.getEndTime() == null) continue;
            LocalDate d = t.getWorkingDate();
            LocalTime cur = t.getStartTime();
            while (cur.isBefore(t.getEndTime())) {
                String key = d.toString() + "#" + cur.toString();
                occupied.add(key);
                cur = cur.plusMinutes(30);
            }
        }

        // Build free slot list from candidates
        java.util.List<Slot> freeList = new java.util.ArrayList<>();
        for (Slot s : candidates) {
            String key = s.date.toString() + "#" + s.start.toString();
            if (!occupied.contains(key)) freeList.add(s);
        }

        int freeCount = freeList.size();
        if (freeCount >= neededSlots) {
            // enough free slots (non-contiguous) - accept (segments created later)
            Slot first = freeList.get(0);
            task.setWorkingDate(first.date);
            task.setStartTime(first.start);
            task.setEndTime(first.start.plusMinutes(Math.min(30, neededMinutes)));
            return true;
        }

        // Try moving lower priority tasks: attempt to relocate them once
        for (Task t : existing) {
            if (priorityValue(t) >= priorityValue(task)) continue; // only move lower priority
            if (t.getWorkingDate() == null || t.getStartTime() == null || t.getEndTime() == null) continue;
            int tNeeded = (int) Math.ceil(( (t.getEstimatedTime() != null ? t.getEstimatedTime() : 0) ) / 30.0);
            // free current slots of t
            java.util.List<String> tKeys = new java.util.ArrayList<>();
            LocalDate td = t.getWorkingDate();
            LocalTime tc = t.getStartTime();
            while (tc.isBefore(t.getEndTime())) {
                tKeys.add(td.toString() + "#" + tc.toString());
                tc = tc.plusMinutes(30);
            }
            for (String k : tKeys) occupied.remove(k);

            // try to find alternative slots for t within its due date
            boolean moved = false;
            java.util.List<Slot> alt = findSlotsForTask(t, avail);
            if (alt.size() >= tNeeded) {
                // allocate first tNeeded slots to t
                Slot s1 = alt.get(0);
                t.setWorkingDate(s1.date);
                t.setStartTime(s1.start);
                t.setEndTime(alt.get(tNeeded-1).end);
                taskRepository.save(t);
                // mark occupied with new slots
                LocalTime cur = t.getStartTime();
                while (cur.isBefore(t.getEndTime())) {
                    occupied.add(t.getWorkingDate().toString() + "#" + cur.toString());
                    cur = cur.plusMinutes(30);
                }
                // update freeList
                freeList.clear();
                for (Slot s : candidates) {
                    String key = s.date.toString() + "#" + s.start.toString();
                    if (!occupied.contains(key)) freeList.add(s);
                }
                if (freeList.size() >= neededSlots) {
                    // now we can allocate new task
                    return true;
                }
            } else {
                // couldn't move, restore occupied
                for (String k : tKeys) occupied.add(k);
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

    private int priorityValue(Task t) {
        String p = t.getPriority();
        if (p == null) return 1;
        switch (p) {
            case "High": return 3;
            case "Medium": return 2;
            default: return 1;
        }
    }

    // Helper classes and methods for scheduling
    private static class Slot {
        LocalDate date;
        LocalTime start;
        LocalTime end;
        Slot(LocalDate d, LocalTime s, LocalTime e) { date = d; start = s; end = e; }
    }

    private static class Block {
        List<Slot> slots = new java.util.ArrayList<>();
    }

    private List<Block> findContiguousBlocks(List<Slot> slots, int neededSlots) {
        List<Block> result = new java.util.ArrayList<>();
        if (slots.isEmpty()) return result;
        slots.sort(java.util.Comparator.comparing((Slot s) -> s.date).thenComparing(s -> s.start));
        Block current = new Block();
        for (int i = 0; i < slots.size(); i++) {
            Slot s = slots.get(i);
            if (current.slots.isEmpty()) {
                current.slots.add(s);
            } else {
                Slot last = current.slots.get(current.slots.size()-1);
                if (s.date.equals(last.date) && s.start.equals(last.end)) {
                    current.slots.add(s);
                } else {
                    if (!current.slots.isEmpty()) result.add(current);
                    current = new Block();
                    current.slots.add(s);
                }
            }
        }
        if (!current.slots.isEmpty()) result.add(current);
        return result;
    }

    private java.util.List<Slot> findSlotsForTask(Task t, Map<String, java.util.List<String>> avail) {
        java.util.List<Slot> out = new java.util.ArrayList<>();
        if (t.getDueDate() == null) return out;
        LocalDate start = LocalDate.now().plusDays(1);
        LocalDate end = t.getDueDate();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            String key = dayNameFor(d.getDayOfWeek().getValue() % 7);
            java.util.List<String> free = avail.get(key);
            if (free == null) continue;
            for (String slot : free) {
                String[] parts = slot.split("-");
                if (parts.length != 2) continue;
                try {
                    LocalTime s = LocalTime.parse(parts[0]);
                    LocalTime e = LocalTime.parse(parts[1]);
                    LocalTime cur = s;
                    while (cur.plusMinutes(30).isBefore(e) || cur.plusMinutes(30).equals(e)) {
                        out.add(new Slot(d, cur, cur.plusMinutes(30)));
                        cur = cur.plusMinutes(30);
                    }
                } catch (Exception ex) { }
            }
        }
        return out;
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

        if (task.getEstimatedTime() == null || task.getEstimatedTime() <= 0) {
            throw new IllegalArgumentException("El tiempo estimado debe ser mayor a 0");
        }
    }
}