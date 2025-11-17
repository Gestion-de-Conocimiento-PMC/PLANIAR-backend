package com.planiarback.planiar.service;

import com.planiarback.planiar.model.Task;
import com.planiarback.planiar.model.User;
import com.planiarback.planiar.repository.TaskRepository;
import com.planiarback.planiar.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.List;
import java.util.Objects;

import com.planiarback.planiar.service.AIPlannerService;
import java.util.Optional;

@Service
public class TaskService {
    private static final Logger logger = LoggerFactory.getLogger(TaskService.class);

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final AIPlannerService aiPlannerService;
    private final TransactionTemplate transactionTemplate;

    public TaskService(TaskRepository taskRepository, UserRepository userRepository, UserService userService, AIPlannerService aiPlannerService, PlatformTransactionManager txManager) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.userService = userService;
        this.aiPlannerService = aiPlannerService;
        this.transactionTemplate = new TransactionTemplate(txManager);
    }

    /**
     * Crear una nueva tarea
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Task createTask(Task task, Long userId) {
        // Simplified, memory-light create flow for low-memory deployment
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con id: " + userId));

        task.setUser(user);
        validateTask(task);
        // Ensure user's availability is up to date
        userService.recalculateAvailableHours(user);

        // If the task has no explicit schedule, do a quick lightweight assignment and then
        // rely on the AI planner to fully reorganize (avoid heavy local scheduling).
        if (task.getWorkingDate() == null && task.getStartTime() == null && task.getEndTime() == null) {
            try {
                boolean assigned = quickAssignDueDate(task, user);
                if (assigned) {
                    logger.info("quickAssignDueDate assigned task '{}' for user {} -> workingDate={}, start={}, end={}",
                            task.getTitle(), userId, task.getWorkingDate(), task.getStartTime(), task.getEndTime());
                } else {
                    logger.info("quickAssignDueDate did not find block for task '{}' user {} (will still save and call AI)", task.getTitle(), userId);
                }
            } catch (Exception ex) {
                logger.warn("quickAssignDueDate failed for user {}: {}", userId, ex.getMessage());
            }
        }

        // Save the (possibly minimally assigned) task quickly to avoid keeping big in-memory structures
        Task savedParent = saveTaskQuickTransactional(task);

        // Invoke AI planner outside of any DB transaction to avoid holding a DB connection
        List<Task> planned = null;
        try {
            logger.info("Invoking AIPlannerService.planTasks for user {} with {} existing tasks", user.getId(), taskRepository.countByUserId(user.getId()));
            List<Task> all = safeFindByUserId(user.getId());
            // ensure the saved parent is present in the list
            boolean containsParent = all.stream().anyMatch(t -> t.getId() != null && t.getId().equals(savedParent.getId()));
            if (!containsParent) all.add(savedParent);

            planned = aiPlannerService.planTasks(all, user.getAvailableHours());
            logger.info("AIPlannerService.planTasks returned {} planned items for user {}", planned == null ? 0 : planned.size(), user.getId());
        } catch (Exception ex) {
            logger.error("AI planner call failed for user {}: {}", user.getId(), ex.getMessage(), ex);
            // In case AI planner fails, return the saved parent and let client retry planning later
            userService.recalculateAvailableHours(user);
            return savedParent;
        }

        // Persist the planner output in a new transaction
        try {
            applyPlannedTasksTransactional(planned, user, savedParent);
        } catch (Exception ex) {
            logger.error("Error persisting planned tasks for user {}: {}", user.getId(), ex.getMessage(), ex);
            // still return savedParent as best-effort
        }

        // Recalculate availability after applying the plan
        userService.recalculateAvailableHours(user);

        // Return the most up-to-date version of the saved task
        if (savedParent.getId() != null) return taskRepository.findById(savedParent.getId()).orElse(savedParent);
        List<Task> candidates = taskRepository.findByUserIdAndTitle(user.getId(), savedParent.getTitle()).map(java.util.List::of).orElseGet(java.util.ArrayList::new);
        if (!candidates.isEmpty()) return candidates.get(0);
        return savedParent;
    }

    // Use TransactionTemplate to start new transactions even when invoked from same class
    protected Task saveTaskQuickTransactional(Task task) {
        return transactionTemplate.execute(status -> safeSave(task));
    }

    protected void applyPlannedTasksTransactional(List<Task> planned, User user, Task savedParent) {
        if (planned == null) return;
        transactionTemplate.execute(status -> {
            for (Task p : planned) {
                try {
                    if (p.getUser() == null) p.setUser(user);

                    if (p.getId() != null && taskRepository.existsById(p.getId())) {
                        Task existing = taskRepository.findById(p.getId()).orElse(null);
                        if (existing != null) {
                            existing.setWorkingDate(p.getWorkingDate());
                            existing.setStartTime(p.getStartTime());
                            existing.setEndTime(p.getEndTime());
                            existing.setPriority(p.getPriority());
                            existing.setEstimatedTime(p.getEstimatedTime());
                            existing.setDescription(p.getDescription());
                            existing.setType(p.getType());
                            existing.setState(p.getState());
                            existing.setDueDate(p.getDueDate());
                            existing.setDueTime(p.getDueTime());
                            safeSave(existing);
                            continue;
                        }
                    }

                    Optional<Task> match = taskRepository.findByUserIdAndTitle(user.getId(), p.getTitle());
                    if (match.isPresent()) {
                        Task existing = match.get();
                        existing.setWorkingDate(p.getWorkingDate());
                        existing.setStartTime(p.getStartTime());
                        existing.setEndTime(p.getEndTime());
                        existing.setPriority(p.getPriority());
                        existing.setEstimatedTime(p.getEstimatedTime());
                        existing.setDescription(p.getDescription());
                        existing.setType(p.getType());
                        existing.setState(p.getState());
                        existing.setDueDate(p.getDueDate());
                        existing.setDueTime(p.getDueTime());
                        safeSave(existing);
                    } else {
                        p.setId(null);
                        p.setUser(user);
                        safeSave(p);
                    }
                } catch (Exception ex) {
                    logger.error("Error persisting planned task for user {}: {}", user.getId(), ex.getMessage(), ex);
                }
            }
            return null;
        });
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
    List<Task> existing = safeFindByUserId(user.getId());
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
                safeSave(t);
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

        // Safe fallback: if analysis didn't assign the task, try to find the latest
        // contiguous block of free 30-min slots (ending as late as possible) before the deadline
        // that can accommodate the neededSlots and assign it.
        for (LocalDate d = deadline; !d.isBefore(startDate); d = d.minusDays(1)) {
            String dayKey = dayNameFor(d.getDayOfWeek().getValue() % 7);
            java.util.List<String> freeSlots = avail.get(dayKey);
            if (freeSlots == null) continue;

            // Build list of available 30-min Slot for this day excluding occupied
            java.util.List<Slot> dayCandidates = new java.util.ArrayList<>();
            for (String slot : freeSlots) {
                String[] parts = slot.split("-");
                if (parts.length != 2) continue;
                try {
                    LocalTime s = LocalTime.parse(parts[0]);
                    LocalTime e = LocalTime.parse(parts[1]);
                    // trim if on deadline day
                    if (d.equals(deadline) && e.isAfter(deadlineTime)) e = deadlineTime;
                    if (!s.isBefore(e)) continue;
                    LocalTime cur = s;
                    while (cur.plusMinutes(30).isBefore(e) || cur.plusMinutes(30).equals(e)) {
                        String key = d.toString() + "#" + cur.toString();
                        if (!occupied.contains(key)) dayCandidates.add(new Slot(d, cur, cur.plusMinutes(30)));
                        cur = cur.plusMinutes(30);
                    }
                } catch (Exception ex) {
                    // ignore parse
                }
            }

            if (dayCandidates.isEmpty()) continue;

            // find contiguous blocks within this day's candidates
            List<Block> dayBlocks = findContiguousBlocks(dayCandidates, neededSlots);
            if (!dayBlocks.isEmpty()) {
                // pick the block that ends the latest (best safe fallback)
                Block best = dayBlocks.stream()
                        .max(java.util.Comparator.comparing(b -> b.slots.get(b.slots.size() - 1).end))
                        .orElse(dayBlocks.get(0));

                int startIndex = Math.max(0, best.slots.size() - neededSlots);
                List<Slot> chosen = best.slots.subList(startIndex, Math.min(startIndex + neededSlots, best.slots.size()));
                if (!chosen.isEmpty()) {
                    task.setWorkingDate(chosen.get(0).date);
                    task.setStartTime(chosen.get(0).start);
                    task.setEndTime(chosen.get(chosen.size() - 1).end);
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
     * Quick forced assignment: try to set workingDate/startTime/endTime on the task to a block
     * on the dueDate that fits (or partially fits) the estimatedTime ending before dueTime.
     * This is a light-weight, fast fallback to avoid long scheduling work on rate-limited servers.
     */
    private boolean quickAssignDueDate(Task task, User user) {
        if (task.getDueDate() == null) return false;
        if (task.getEstimatedTime() == null || task.getEstimatedTime() <= 0) return false;

        Map<String, java.util.List<String>> avail = user.getAvailableHours();
        if (avail == null) return false;

        int neededSlots = (int) Math.ceil(task.getEstimatedTime() / 30.0);
        LocalDate d = task.getDueDate();
        LocalTime deadlineTime = task.getDueTime() != null ? task.getDueTime() : LocalTime.of(23, 59);

        // build occupied slots for user
        java.util.Set<String> occupied = new java.util.HashSet<>();
    List<Task> existing = safeFindByUserId(user.getId());
        for (Task t : existing) {
            if (t.getWorkingDate() == null || t.getStartTime() == null || t.getEndTime() == null) continue;
            LocalDate td = t.getWorkingDate();
            LocalTime cur = t.getStartTime();
            while (cur.isBefore(t.getEndTime())) {
                occupied.add(td.toString() + "#" + cur.toString());
                cur = cur.plusMinutes(30);
            }
        }

        String dayKey = dayNameFor(d.getDayOfWeek().getValue() % 7);
        java.util.List<String> freeSlots = avail.get(dayKey);
        if (freeSlots == null) return false;

        // collect available 30-min slots for dueDate excluding occupied and trimming by dueTime
        java.util.List<Slot> dayCandidates = new java.util.ArrayList<>();
        for (String slot : freeSlots) {
            String[] parts = slot.split("-");
            if (parts.length != 2) continue;
            try {
                LocalTime s = LocalTime.parse(parts[0]);
                LocalTime e = LocalTime.parse(parts[1]);
                if (e.isAfter(deadlineTime)) e = deadlineTime;
                if (!s.isBefore(e)) continue;
                LocalTime cur = s;
                while (cur.plusMinutes(30).isBefore(e) || cur.plusMinutes(30).equals(e)) {
                    String key = d.toString() + "#" + cur.toString();
                    if (!occupied.contains(key)) dayCandidates.add(new Slot(d, cur, cur.plusMinutes(30)));
                    cur = cur.plusMinutes(30);
                }
            } catch (Exception ex) { }
        }

        if (dayCandidates.isEmpty()) return false;

        // find contiguous blocks and pick the one that ends latest
        List<Block> dayBlocks = findContiguousBlocks(dayCandidates, neededSlots);
        if (!dayBlocks.isEmpty()) {
            Block best = dayBlocks.stream()
                    .max(java.util.Comparator.comparing(b -> b.slots.get(b.slots.size() - 1).end))
                    .orElse(dayBlocks.get(0));

            // if block covers neededSlots, assign last neededSlots of block
            if (best.slots.size() >= neededSlots) {
                int startIndex = Math.max(0, best.slots.size() - neededSlots);
                List<Slot> chosen = best.slots.subList(startIndex, startIndex + neededSlots);
                task.setWorkingDate(d);
                task.setStartTime(chosen.get(0).start);
                task.setEndTime(chosen.get(chosen.size() - 1).end);
                return true;
            }

            // otherwise assign the entire best block (partial fit) to at least set the fields
            List<Slot> chosen = best.slots;
            task.setWorkingDate(d);
            task.setStartTime(chosen.get(0).start);
            task.setEndTime(chosen.get(chosen.size() - 1).end);
            return true;
        }

        return false;
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
        return safeFindByUserId(userId);
    }

    // Wrapper to fetch tasks with logging and defensive handling to help debug production failures
    private List<Task> safeFindByUserId(Long userId) {
        try {
            List<Task> list = taskRepository.findByUserId(userId);
            if (list == null) {
                logger.debug("safeFindByUserId returned null for userId {}", userId);
                return new java.util.ArrayList<>();
            }
            logger.debug("safeFindByUserId userId {} returned {} tasks", userId, list.size());
            return list;
        } catch (Exception e) {
            logger.error("Error fetching tasks for user {}: {}", userId, e.getMessage(), e);
            throw e;
        }
    }

    // Save wrapper that logs task details on error to help debug production failures
    private Task safeSave(Task task) {
        try {
            logger.debug("Saving task (title={}, userId={}, dueDate={}, workingDate={}, start={}, end={})",
                    task == null ? null : task.getTitle(),
                    task == null || task.getUser() == null ? null : task.getUser().getId(),
                    task == null ? null : task.getDueDate(),
                    task == null ? null : task.getWorkingDate(),
                    task == null ? null : task.getStartTime(),
                    task == null ? null : task.getEndTime()
            );
            return taskRepository.save(task);
        } catch (Exception e) {
            try {
                logger.error("Failed to save task: {}", task == null ? "<null>" : task.toString(), e);
            } catch (Exception ex) {
                logger.error("Failed to save task and toString() also failed", e);
            }
            throw e;
        }
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
    @Transactional
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
    Task saved = safeSave(task);
        userService.recalculateAvailableHours(task.getUser());
        return saved;
    }

    /**
     * Eliminar una tarea
     */
    @Transactional
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
    @Transactional
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
    @Transactional
    public Task assignTaskToClass(Long taskId, Long classId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Tarea no encontrada con id: " + taskId));

        task.setClassId(classId);
    Task saved = safeSave(task);
        userService.recalculateAvailableHours(task.getUser());
        return saved;
    }

    /**
     * Remover tarea de una clase
     */
    @Transactional
    public Task removeTaskFromClass(Long taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Tarea no encontrada con id: " + taskId));

        task.setClassId(null);
    Task saved = safeSave(task);
        userService.recalculateAvailableHours(task.getUser());
        return saved;
    }

    /**
     * Cambiar estado de una tarea
     */
    @Transactional
    public Task updateTaskState(Long taskId, String newState) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Tarea no encontrada con id: " + taskId));

        task.setState(newState);
    Task saved = safeSave(task);
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