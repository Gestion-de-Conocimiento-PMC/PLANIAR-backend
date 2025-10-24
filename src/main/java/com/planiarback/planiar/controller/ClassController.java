package com.planiarback.planiar.controller;

import com.planiarback.planiar.model.Class;
import com.planiarback.planiar.service.ClassService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/classes")
public class ClassController {

    private final ClassService classService;

    public ClassController(ClassService classService) {
        this.classService = classService;
    }

    /**
     * Crear una nueva clase
     * POST /api/classes/user/{userId}
     */
    @PostMapping("/user/{userId}")
    public ResponseEntity<?> createClass(@PathVariable Long userId, @RequestBody Class classEntity) {
        try {
            Class createdClass = classService.createClass(classEntity, userId);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdClass);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Obtener todas las clases
     * GET /api/classes
     */
    @GetMapping
    public ResponseEntity<List<Class>> getAllClasses() {
        List<Class> classes = classService.getAllClasses();
        return ResponseEntity.ok(classes);
    }

    /**
     * Obtener todas las clases de un usuario
     * GET /api/classes/user/{userId}
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Class>> getAllClassesByUser(@PathVariable Long userId) {
        List<Class> classes = classService.getAllClassesByUser(userId);
        return ResponseEntity.ok(classes);
    }

    /**
     * Obtener clases ordenadas alfabéticamente
     * GET /api/classes/user/{userId}/ordered
     */
    @GetMapping("/user/{userId}/ordered")
    public ResponseEntity<List<Class>> getClassesOrdered(@PathVariable Long userId) {
        List<Class> classes = classService.getClassesOrderedByTitle(userId);
        return ResponseEntity.ok(classes);
    }

    /**
     * Obtener clases ordenadas por fecha
     * GET /api/classes/user/{userId}/ordered-by-date
     */
    @GetMapping("/user/{userId}/ordered-by-date")
    public ResponseEntity<List<Class>> getClassesOrderedByDate(@PathVariable Long userId) {
        List<Class> classes = classService.getClassesOrderedByDate(userId);
        return ResponseEntity.ok(classes);
    }

    /**
     * Obtener una clase por ID
     * GET /api/classes/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getClassById(@PathVariable Long id) {
        return classService.getClassById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Obtener clase por título
     * GET /api/classes/title/{title}
     */
    @GetMapping("/title/{title}")
    public ResponseEntity<?> getClassByTitle(@PathVariable String title) {
        return classService.getClassByTitle(title)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Obtener clase por título de un usuario
     * GET /api/classes/user/{userId}/title/{title}
     */
    @GetMapping("/user/{userId}/title/{title}")
    public ResponseEntity<?> getClassByTitleAndUser(@PathVariable Long userId, @PathVariable String title) {
        return classService.getClassByTitleAndUser(userId, title)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Buscar clases por título
     * GET /api/classes/search?title=fisica
     */
    @GetMapping("/search")
    public ResponseEntity<List<Class>> searchClassesByTitle(@RequestParam String title) {
        List<Class> classes = classService.searchClassesByTitle(title);
        return ResponseEntity.ok(classes);
    }

    /**
     * Buscar clases de un usuario por título
     * GET /api/classes/user/{userId}/search?title=fisica
     */
    @GetMapping("/user/{userId}/search")
    public ResponseEntity<List<Class>> searchClassesByTitleAndUser(
            @PathVariable Long userId,
            @RequestParam String title) {
        List<Class> classes = classService.searchClassesByTitleAndUser(userId, title);
        return ResponseEntity.ok(classes);
    }

    /**
     * Obtener clases activas en una fecha específica
     * GET /api/classes/user/{userId}/active?date=2024-03-15
     */
    @GetMapping("/user/{userId}/active")
    public ResponseEntity<List<Class>> getActiveClassesOnDate(
            @PathVariable Long userId,
            @RequestParam(required = false) String date) {
        LocalDate targetDate = date != null ? LocalDate.parse(date) : LocalDate.now();
        List<Class> classes = classService.getActiveClassesOnDate(userId, targetDate);
        return ResponseEntity.ok(classes);
    }

    /**
     * Obtener clases activas hoy
     * GET /api/classes/user/{userId}/today
     */
    @GetMapping("/user/{userId}/today")
    public ResponseEntity<List<Class>> getActiveClassesToday(@PathVariable Long userId) {
        List<Class> classes = classService.getActiveClassesToday(userId);
        return ResponseEntity.ok(classes);
    }

    /**
     * Obtener clases por color
     * GET /api/classes/user/{userId}/color/{color}
     */
    @GetMapping("/user/{userId}/color/{color}")
    public ResponseEntity<List<Class>> getClassesByColor(
            @PathVariable Long userId,
            @PathVariable String color) {
        List<Class> classes = classService.getClassesByColor(userId, color);
        return ResponseEntity.ok(classes);
    }

    /**
     * Obtener clases en un rango de fechas
     * GET /api/classes/user/{userId}/range?startDate=2024-03-01&endDate=2024-03-31
     */
    @GetMapping("/user/{userId}/range")
    public ResponseEntity<List<Class>> getClassesInRange(
            @PathVariable Long userId,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);
        List<Class> classes = classService.getClassesInRange(userId, start, end);
        return ResponseEntity.ok(classes);
    }

    /**
     * Obtener clases futuras
     * GET /api/classes/user/{userId}/future
     */
    @GetMapping("/user/{userId}/future")
    public ResponseEntity<List<Class>> getFutureClasses(@PathVariable Long userId) {
        List<Class> classes = classService.getFutureClasses(userId);
        return ResponseEntity.ok(classes);
    }

    /**
     * Obtener clases pasadas
     * GET /api/classes/user/{userId}/past
     */
    @GetMapping("/user/{userId}/past")
    public ResponseEntity<List<Class>> getPastClasses(@PathVariable Long userId) {
        List<Class> classes = classService.getPastClasses(userId);
        return ResponseEntity.ok(classes);
    }

    /**
     * Buscar clases por profesor
     * GET /api/classes/user/{userId}/professor?professor=Garcia
     */
    @GetMapping("/user/{userId}/professor")
    public ResponseEntity<List<Class>> searchClassesByProfessor(
            @PathVariable Long userId,
            @RequestParam String professor) {
        List<Class> classes = classService.searchClassesByProfessor(userId, professor);
        return ResponseEntity.ok(classes);
    }

    /**
     * Buscar clases por salón
     * GET /api/classes/user/{userId}/room?room=ML203
     */
    @GetMapping("/user/{userId}/room")
    public ResponseEntity<List<Class>> searchClassesByRoom(
            @PathVariable Long userId,
            @RequestParam String room) {
        List<Class> classes = classService.searchClassesByRoom(userId, room);
        return ResponseEntity.ok(classes);
    }

    /**
     * Obtener clases para un día de la semana específico
     * GET /api/classes/user/{userId}/day-of-week/{dayOfWeek}
     * dayOfWeek: 0-6 (Dom-Sab)
     */
    @GetMapping("/user/{userId}/day-of-week/{dayOfWeek}")
    public ResponseEntity<?> getClassesByDayOfWeek(
            @PathVariable Long userId,
            @PathVariable int dayOfWeek) {
        try {
            if (dayOfWeek < 0 || dayOfWeek > 6) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Día de la semana inválido. Use 0-6 (Dom-Sab)"));
            }
            List<Class> classes = classService.getClassesByDayOfWeek(userId, dayOfWeek);
            return ResponseEntity.ok(classes);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al obtener clases"));
        }
    }

    /**
     * Obtener clases de un usuario en una fecha específica
     * GET /api/classes/user/{userId}/date/{date}
     */
    @GetMapping("/user/{userId}/date/{date}")
    public ResponseEntity<List<Class>> getClassesByUserAndDate(
            @PathVariable Long userId,
            @PathVariable String date) {
        LocalDate parsedDate = LocalDate.parse(date);
        List<Class> classes = classService.getClassesByUserAndDate(userId, parsedDate);
        return ResponseEntity.ok(classes);
    }

    /**
     * Actualizar una clase
     * PUT /api/classes/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateClass(@PathVariable Long id, @RequestBody Class classDetails) {
        try {
            Class updatedClass = classService.updateClass(id, classDetails);
            return ResponseEntity.ok(updatedClass);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Eliminar una clase
     * DELETE /api/classes/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteClass(@PathVariable Long id) {
        try {
            classService.deleteClass(id);
            return ResponseEntity.ok(Map.of("message", "Clase eliminada exitosamente"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Eliminar todas las clases de un usuario
     * DELETE /api/classes/user/{userId}/all
     */
    @DeleteMapping("/user/{userId}/all")
    public ResponseEntity<?> deleteAllUserClasses(@PathVariable Long userId) {
        classService.deleteAllUserClasses(userId);
        return ResponseEntity.ok(Map.of("message", "Todas las clases eliminadas exitosamente"));
    }

    /**
     * Contar clases de un usuario
     * GET /api/classes/user/{userId}/count
     */
    @GetMapping("/user/{userId}/count")
    public ResponseEntity<Map<String, Long>> countUserClasses(@PathVariable Long userId) {
        long count = classService.countUserClasses(userId);
        return ResponseEntity.ok(Map.of("count", count));
    }

    /**
     * Verificar si existe una clase con ese título
     * GET /api/classes/exists?title=Fisica
     */
    @GetMapping("/exists")
    public ResponseEntity<Map<String, Boolean>> checkClassExists(@RequestParam String title) {
        boolean exists = classService.existsByTitle(title);
        return ResponseEntity.ok(Map.of("exists", exists));
    }

    /**
     * Verificar si un usuario tiene una clase con ese título
     * GET /api/classes/user/{userId}/exists?title=Fisica
     */
    @GetMapping("/user/{userId}/exists")
    public ResponseEntity<Map<String, Boolean>> checkUserClassExists(
            @PathVariable Long userId,
            @RequestParam String title) {
        boolean exists = classService.existsByUserIdAndTitle(userId, title);
        return ResponseEntity.ok(Map.of("exists", exists));
    }

    /**
     * Obtener horarios de inicio de una clase
     * GET /api/classes/{id}/start-times
     */
    @GetMapping("/{id}/start-times")
    public ResponseEntity<?> getStartTimes(@PathVariable Long id) {
        return classService.getClassById(id)
                .map(classEntity -> {
                    List<LocalTime> startTimes = classService.getStartTimesAsList(classEntity);
                    return ResponseEntity.ok(startTimes);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Obtener horarios de fin de una clase
     * GET /api/classes/{id}/end-times
     */
    @GetMapping("/{id}/end-times")
    public ResponseEntity<?> getEndTimes(@PathVariable Long id) {
        return classService.getClassById(id)
                .map(classEntity -> {
                    List<LocalTime> endTimes = classService.getEndTimesAsList(classEntity);
                    return ResponseEntity.ok(endTimes);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Obtener lista de profesores de una clase
     * GET /api/classes/{id}/professors
     */
    @GetMapping("/{id}/professors")
    public ResponseEntity<?> getProfessors(@PathVariable Long id) {
        return classService.getClassById(id)
                .map(classEntity -> {
                    List<String> professors = classService.getProfessorsAsList(classEntity);
                    return ResponseEntity.ok(professors);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Obtener lista de salones de una clase
     * GET /api/classes/{id}/rooms
     */
    @GetMapping("/{id}/rooms")
    public ResponseEntity<?> getRooms(@PathVariable Long id) {
        return classService.getClassById(id)
                .map(classEntity -> {
                    List<String> rooms = classService.getRoomsAsList(classEntity);
                    return ResponseEntity.ok(rooms);
                })
                .orElse(ResponseEntity.notFound().build());
    }
}