package com.planiarback.planiar.service;

import com.planiarback.planiar.model.Activity;
import com.planiarback.planiar.model.Class;
import com.planiarback.planiar.model.Task;
import com.planiarback.planiar.model.User;
import com.planiarback.planiar.repository.ActivityRepository;
import com.planiarback.planiar.repository.ClassRepository;
import com.planiarback.planiar.repository.TaskRepository;
import com.planiarback.planiar.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final ClassRepository classRepository;
    private final ActivityRepository activityRepository;

    public UserService(UserRepository userRepository,
                       TaskRepository taskRepository,
                       ClassRepository classRepository,
                       ActivityRepository activityRepository) {
        this.userRepository = userRepository;
        this.taskRepository = taskRepository;
        this.classRepository = classRepository;
        this.activityRepository = activityRepository;
    }

    /**
     * Crear un nuevo usuario
     */
    public User createUser(User user) {
        validateUser(user);
        
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new IllegalArgumentException("El nombre de usuario ya existe");
        }
        
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new IllegalArgumentException("El email ya está registrado");
        }
        
        if (user.getType() == null || user.getType().trim().isEmpty()) {
            user.setType("user");
        }
        
        return userRepository.save(user);
    }

    /**
     * Obtener todos los usuarios
     */
    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    /**
     * Obtener un usuario por ID
     */
    @Transactional(readOnly = true)
    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    /**
     * Obtener usuario por username
     */
    @Transactional(readOnly = true)
    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    /**
     * Obtener usuario por email
     */
    @Transactional(readOnly = true)
    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * Buscar usuarios por username
     */
    @Transactional(readOnly = true)
    public List<User> searchUsersByUsername(String username) {
        return userRepository.findByUsernameContainingIgnoreCase(username);
    }

    /**
     * Buscar usuarios por email
     */
    @Transactional(readOnly = true)
    public List<User> searchUsersByEmail(String email) {
        return userRepository.findByEmailContainingIgnoreCase(email);
    }

    /**
     * Obtener todos los usuarios administradores
     */
    @Transactional(readOnly = true)
    public List<User> getAllAdminUsers() {
        return userRepository.findByType("admin");
    }

    /**
     * Obtener todos los usuarios no administradores
     */
    @Transactional(readOnly = true)
    public List<User> getAllNonAdminUsers() {
        return userRepository.findByType("user");
    }

    /**
     * Verificar si existe un usuario con ese username
     */
    @Transactional(readOnly = true)
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    /**
     * Verificar si existe un usuario con ese email
     */
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    /**
     * Actualizar un usuario
     */
    public User updateUser(Long id, User userDetails) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con id: " + id));

        // Verificar si el nuevo username ya existe (y no es del mismo usuario)
        if (!user.getUsername().equals(userDetails.getUsername()) &&
            userRepository.existsByUsername(userDetails.getUsername())) {
            throw new IllegalArgumentException("El nombre de usuario ya existe");
        }

        // Verificar si el nuevo email ya existe (y no es del mismo usuario)
        if (!user.getEmail().equals(userDetails.getEmail()) &&
            userRepository.existsByEmail(userDetails.getEmail())) {
            throw new IllegalArgumentException("El email ya está registrado");
        }

        user.setUsername(userDetails.getUsername());
        user.setEmail(userDetails.getEmail());
        user.setPassword(userDetails.getPassword());
        
        if (userDetails.getType() != null) {
            user.setType(userDetails.getType());
        }

        validateUser(user);
        return userRepository.save(user);
    }

    /**
     * Actualizar solo el username
     */
    public User updateUsername(Long id, String newUsername) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con id: " + id));

        if (!user.getUsername().equals(newUsername) &&
            userRepository.existsByUsername(newUsername)) {
            throw new IllegalArgumentException("El nombre de usuario ya existe");
        }

        user.setUsername(newUsername);
        return userRepository.save(user);
    }

    /**
     * Actualizar solo el email
     */
    public User updateEmail(Long id, String newEmail) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con id: " + id));

        if (!user.getEmail().equals(newEmail) &&
            userRepository.existsByEmail(newEmail)) {
            throw new IllegalArgumentException("El email ya está registrado");
        }

        user.setEmail(newEmail);
        return userRepository.save(user);
    }

    /**
     * Actualizar solo la contraseña
     */
    public User updatePassword(Long id, String newPassword) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con id: " + id));

        if (newPassword == null || newPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("La contraseña no puede estar vacía");
        }

        user.setPassword(newPassword);
        return userRepository.save(user);
    }
    /**
     * Cambiar entre admin y user (toggle)
     */
    public User toggleAdminStatus(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con id: " + id));

        if ("admin".equalsIgnoreCase(user.getType())) {
            user.setType("user");
        } else {
            user.setType("admin");
        }
        return userRepository.save(user);
    }

    /**
     * Establecer como administrador
     */
    public User setAsAdmin(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con id: " + id));

        user.setType("admin");
        return userRepository.save(user);
    }

    /**
     * Remover como administrador (dejar como 'user')
     */
    public User removeAsAdmin(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con id: " + id));

        user.setType("user");
        return userRepository.save(user);
    }

    /**
     * Eliminar un usuario
     */
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new IllegalArgumentException("Usuario no encontrado con id: " + id);
        }
        userRepository.deleteById(id);
    }

    /**
     * Contar todos los usuarios
     */
    @Transactional(readOnly = true)
    public long countAllUsers() {
        return userRepository.count();
    }

    /**
     * Contar usuarios administradores
     */
    @Transactional(readOnly = true)
    public long countAdminUsers() {
        return userRepository.countByType("admin");
    }

    /**
     * Contar usuarios no administradores
     */
    @Transactional(readOnly = true)
    public long countNonAdminUsers() {
        return userRepository.countByType("user");
    }

    /**
     * Recalculate available hours for a user based on classes, activities and tasks.
     * Assumptions:
     * - available hours per day = 24 - occupied hours
     * - day keys use: SUN,MON,TUE,WED,THU,FRI,SAT
     * - classes and activities have days strings like "0,1,1,0,1,0,0" and start_times/end_times lists
     * - tasks use workingDate if present, otherwise dueDate; estimatedTime is in minutes
     */
    public void recalculateAvailableHours(User user) {
        if (user == null || user.getId() == null) return;

    Map<String, java.util.List<String>> result = new HashMap<>();
        String[] dayNames = new String[]{"SUN","MON","TUE","WED","THU","FRI","SAT"};
    // occupied intervals per day -> mark occupied hourly slots
    boolean[][] occupied = new boolean[7][24];

        // Classes
        List<Class> classes = classRepository.findByUserId(user.getId());
        for (Class c : classes) {
            if (c.getDays() == null || c.getStartTimes() == null || c.getEndTimes() == null) continue;
            String[] days = c.getDays().split(",");
            String[] starts = c.getStartTimes().split(",");
            String[] ends = c.getEndTimes().split(",");
            for (int i = 0; i < days.length && i < 7; i++) {
                if (!"1".equals(days[i].trim())) continue;
                String s = (i < starts.length) ? starts[i].trim() : (starts.length > 0 ? starts[0].trim() : null);
                String e = (i < ends.length) ? ends[i].trim() : (ends.length > 0 ? ends[0].trim() : null);
                if (s == null || e == null || s.isEmpty() || e.isEmpty()) continue;
                try {
                    LocalTime st = LocalTime.parse(s);
                    LocalTime en = LocalTime.parse(e);
                    // mark hourly slots overlapped by this interval
                    for (int h = 0; h < 24; h++) {
                        LocalTime slotStart = LocalTime.of(h, 0);
                        LocalTime slotEnd = slotStart.plusHours(1);
                        if (st.isBefore(slotEnd) && en.isAfter(slotStart)) {
                            occupied[i][h] = true;
                        }
                    }
                } catch (Exception ex) {
                    // ignore parse errors
                }
            }
        }

        // Activities
        List<Activity> activities = activityRepository.findByUserId(user.getId());
        for (Activity a : activities) {
            if (a.getDays() == null || a.getStartTimes() == null || a.getEndTimes() == null) continue;
            String[] days = a.getDays().split(",");
            String[] starts = a.getStartTimes().split(",");
            String[] ends = a.getEndTimes().split(",");
            for (int i = 0; i < days.length && i < 7; i++) {
                if (!"1".equals(days[i].trim())) continue;
                String s = (i < starts.length) ? starts[i].trim() : (starts.length > 0 ? starts[0].trim() : null);
                String e = (i < ends.length) ? ends[i].trim() : (ends.length > 0 ? ends[0].trim() : null);
                if (s == null || e == null || s.isEmpty() || e.isEmpty()) continue;
                try {
                    LocalTime st = LocalTime.parse(s);
                    LocalTime en = LocalTime.parse(e);
                    for (int h = 0; h < 24; h++) {
                        LocalTime slotStart = LocalTime.of(h, 0);
                        LocalTime slotEnd = slotStart.plusHours(1);
                        if (st.isBefore(slotEnd) && en.isAfter(slotStart)) {
                            occupied[i][h] = true;
                        }
                    }
                } catch (Exception ex) {
                    // ignore parse errors
                }
            }
        }

        // Tasks: use workingDate and startTime/endTime when available
        List<Task> tasks = taskRepository.findByUserId(user.getId());
        for (Task t : tasks) {
            LocalDate d = t.getWorkingDate();
            if (d == null) continue; // only consider tasks with a workingDate for scheduling
            int idx = d.getDayOfWeek().getValue() % 7; // Sunday -> 0
            try {
                if (t.getStartTime() != null && t.getEndTime() != null) {
                    LocalTime st = t.getStartTime();
                    LocalTime en = t.getEndTime();
                    for (int h = 0; h < 24; h++) {
                        LocalTime slotStart = LocalTime.of(h, 0);
                        LocalTime slotEnd = slotStart.plusHours(1);
                        if (st.isBefore(slotEnd) && en.isAfter(slotStart)) {
                            occupied[idx][h] = true;
                        }
                    }
                }
            } catch (Exception ex) {
                // ignore
            }
        }
        // Build free hourly slots per day as List<String>
        for (int i = 0; i < 7; i++) {
            java.util.List<String> slots = new java.util.ArrayList<>();
            for (int h = 0; h < 24; h++) {
                if (!occupied[i][h]) {
                    int end = (h + 1) % 24;
                    slots.add(String.format("%02d:00-%02d:00", h, end));
                }
            }
            if (!slots.isEmpty()) {
                result.put(dayNames[i], slots);
            }
        }

        // If user has no schedule entries (result empty), default to all days full-day slots
        if (result.isEmpty()) {
            java.util.List<String> all = new java.util.ArrayList<>();
            for (int h = 0; h < 24; h++) {
                int end = (h + 1) % 24;
                all.add(String.format("%02d:00-%02d:00", h, end));
            }
            for (String dName : dayNames) {
                result.put(dName, new java.util.ArrayList<>(all));
            }
        }

        user.setAvailableHours(result);
        userRepository.save(user);
    }

    /**
     * Validar login de usuario
     */
    @Transactional(readOnly = true)
    public Optional<User> login(String username, String password) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre de usuario no puede estar vacío");
        }

        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("La contraseña no puede estar vacía");
        }

        Optional<User> user = userRepository.findByUsername(username);

        if (user.isPresent() && user.get().getPassword().equals(password)) {
            // ensure availableHours is initialized (default 24h for each day when empty)
            User u = user.get();
            if (u.getAvailableHours() == null || u.getAvailableHours().isEmpty()) {
                // recalculate will set defaults when there are no schedule entries
                // call outside read-only context (save will be performed inside recalc)
                recalculateAvailableHours(u);
            }
            return Optional.of(u);
        }

        return Optional.empty();
    }

    /**
     * Validar datos del usuario
     */
    private void validateUser(User user) {
        if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre de usuario no puede estar vacío");
        }

        if (user.getUsername().length() < 3) {
            throw new IllegalArgumentException("El nombre de usuario debe tener al menos 3 caracteres");
        }

        if (user.getUsername().length() > 50) {
            throw new IllegalArgumentException("El nombre de usuario no puede exceder 50 caracteres");
        }

        if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("El email no puede estar vacío");
        }

        if (!user.getEmail().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new IllegalArgumentException("El email no es válido");
        }

        if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
            throw new IllegalArgumentException("La contraseña no puede estar vacía");
        }

        if (user.getPassword().length() < 6) {
            throw new IllegalArgumentException("La contraseña debe tener al menos 6 caracteres");
        }
        
        if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre de usuario no puede estar vacío");
        }

        if (user.getUsername().length() < 3) {
            throw new IllegalArgumentException("El nombre de usuario debe tener al menos 3 caracteres");
        }

        if (user.getUsername().length() > 50) {
            throw new IllegalArgumentException("El nombre de usuario no puede exceder 50 caracteres");
        }

        if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("El email no puede estar vacío");
        }

        if (!user.getEmail().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new IllegalArgumentException("El email no es válido");
        }

        if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
            throw new IllegalArgumentException("La contraseña no puede estar vacía");
        }

        if (user.getPassword().length() < 6) {
            throw new IllegalArgumentException("La contraseña debe tener al menos 6 caracteres");
        }
    }
}