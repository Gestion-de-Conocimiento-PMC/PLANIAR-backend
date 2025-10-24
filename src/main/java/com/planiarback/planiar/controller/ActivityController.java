package com.planiarback.planiar.controller;

import com.planiarback.planiar.model.Activity;
import com.planiarback.planiar.service.ActivityService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/activities")
public class ActivityController {

    private final ActivityService activityService;

    public ActivityController(ActivityService activityService) {
        this.activityService = activityService;
    }

    /**
     * Crear una nueva actividad
     * POST /api/activities/user/{userId}
     */
    @PostMapping("/user/{userId}")
    public ResponseEntity<?> createActivity(@PathVariable Long userId, @RequestBody Activity activity) {
        try {
            Activity createdActivity = activityService.createActivity(activity, userId);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdActivity);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al crear la actividad"));
        }
    }

    /**
     * Obtener una actividad por ID
     * GET /api/activities/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getActivityById(@PathVariable Long id) {
        return activityService.getActivityById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Obtener todas las actividades de un usuario
     * GET /api/activities/user/{userId}
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Activity>> getAllActivitiesByUser(@PathVariable Long userId) {
        List<Activity> activities = activityService.getAllActivitiesByUser(userId);
        return ResponseEntity.ok(activities);
    }

    /**
     * Obtener actividades ordenadas por fecha
     * GET /api/activities/user/{userId}/ordered
     */
    @GetMapping("/user/{userId}/ordered")
    public ResponseEntity<List<Activity>> getActivitiesOrdered(@PathVariable Long userId) {
        List<Activity> activities = activityService.getActivitiesByUserOrderedByDate(userId);
        return ResponseEntity.ok(activities);
    }

    /**
     * Obtener actividades activas en una fecha específica
     * GET /api/activities/user/{userId}/active?date=2024-03-15
     */
    @GetMapping("/user/{userId}/active")
    public ResponseEntity<List<Activity>> getActiveActivitiesOnDate(
            @PathVariable Long userId,
            @RequestParam(required = false) String date) {
        
        LocalDate targetDate = date != null ? LocalDate.parse(date) : LocalDate.now();
        List<Activity> activities = activityService.getActiveActivitiesOnDate(userId, targetDate);
        return ResponseEntity.ok(activities);
    }

    /**
     * Obtener actividades activas hoy
     * GET /api/activities/user/{userId}/today
     */
    @GetMapping("/user/{userId}/today")
    public ResponseEntity<List<Activity>> getActiveActivitiesToday(@PathVariable Long userId) {
        List<Activity> activities = activityService.getActiveActivitiesToday(userId);
        return ResponseEntity.ok(activities);
    }

    /**
     * Obtener el horario de hoy (actividades activas para el día de la semana actual)
     * GET /api/activities/user/{userId}/schedule/today
     */
    @GetMapping("/user/{userId}/schedule/today")
    public ResponseEntity<List<Activity>> getTodaySchedule(@PathVariable Long userId) {
        List<Activity> activities = activityService.getTodaySchedule(userId);
        return ResponseEntity.ok(activities);
    }

    /**
     * Obtener actividades por día de la semana
     * GET /api/activities/user/{userId}/day-of-week/{dayOfWeek}
     * dayOfWeek: MONDAY, TUESDAY, etc.
     */
    @GetMapping("/user/{userId}/day-of-week/{dayOfWeek}")
    public ResponseEntity<?> getActivitiesByDayOfWeek(
            @PathVariable Long userId,
            @PathVariable String dayOfWeek) {
        try {
            DayOfWeek day = DayOfWeek.valueOf(dayOfWeek.toUpperCase());
            List<Activity> activities = activityService.getActivitiesByDayOfWeek(userId, day);
            return ResponseEntity.ok(activities);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Día de la semana inválido: " + dayOfWeek));
        }
    }

    /**
     * Obtener actividades por color
     * GET /api/activities/user/{userId}/color/{color}
     */
    @GetMapping("/user/{userId}/color/{color}")
    public ResponseEntity<List<Activity>> getActivitiesByColor(
            @PathVariable Long userId,
            @PathVariable String color) {
        List<Activity> activities = activityService.getActivitiesByColor(userId, color);
        return ResponseEntity.ok(activities);
    }

    /**
     * Obtener actividades en un rango de fechas
     * GET /api/activities/user/{userId}/range?startDate=2024-03-01&endDate=2024-03-31
     */
    @GetMapping("/user/{userId}/range")
    public ResponseEntity<List<Activity>> getActivitiesInRange(
            @PathVariable Long userId,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);
        List<Activity> activities = activityService.getActivitiesInRange(userId, start, end);
        return ResponseEntity.ok(activities);
    }

    /**
     * Buscar actividades por título
     * GET /api/activities/user/{userId}/search?title=Gimnasio
     */
    @GetMapping("/user/{userId}/search")
    public ResponseEntity<List<Activity>> searchActivitiesByTitle(
            @PathVariable Long userId,
            @RequestParam String title) {
        List<Activity> activities = activityService.searchActivitiesByTitle(userId, title);
        return ResponseEntity.ok(activities);
    }

    /**
     * Obtener actividades futuras
     * GET /api/activities/user/{userId}/future
     */
    @GetMapping("/user/{userId}/future")
    public ResponseEntity<List<Activity>> getFutureActivities(@PathVariable Long userId) {
        List<Activity> activities = activityService.getFutureActivities(userId);
        return ResponseEntity.ok(activities);
    }

    /**
     * Obtener actividades pasadas
     * GET /api/activities/user/{userId}/past
     */
    @GetMapping("/user/{userId}/past")
    public ResponseEntity<List<Activity>> getPastActivities(@PathVariable Long userId) {
        List<Activity> activities = activityService.getPastActivities(userId);
        return ResponseEntity.ok(activities);
    }

    /**
     * Obtener actividades de un usuario en una fecha específica
     * GET /api/activities/user/{userId}/date/{date}
     */
    @GetMapping("/user/{userId}/date/{date}")
    public ResponseEntity<List<Activity>> getActivitiesByUserAndDate(
            @PathVariable Long userId,
            @PathVariable String date) {
        LocalDate parsedDate = LocalDate.parse(date);
        List<Activity> activities = activityService.getActivitiesByUserAndDate(userId, parsedDate);
        return ResponseEntity.ok(activities);
    }

    /**
     * Actualizar una actividad
     * PUT /api/activities/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateActivity(
            @PathVariable Long id,
            @RequestBody Activity activityDetails) {
        try {
            Activity updatedActivity = activityService.updateActivity(id, activityDetails);
            return ResponseEntity.ok(updatedActivity);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al actualizar la actividad"));
        }
    }

    /**
     * Eliminar una actividad
     * DELETE /api/activities/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteActivity(@PathVariable Long id) {
        try {
            activityService.deleteActivity(id);
            return ResponseEntity.ok(Map.of("message", "Actividad eliminada exitosamente"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al eliminar la actividad"));
        }
    }

    /**
     * Eliminar todas las actividades de un usuario
     * DELETE /api/activities/user/{userId}/all
     */
    @DeleteMapping("/user/{userId}/all")
    public ResponseEntity<?> deleteAllUserActivities(@PathVariable Long userId) {
        try {
            activityService.deleteAllUserActivities(userId);
            return ResponseEntity.ok(Map.of("message", "Todas las actividades eliminadas exitosamente"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al eliminar las actividades"));
        }
    }

    /**
     * Contar actividades activas en una fecha
     * GET /api/activities/user/{userId}/count?date=2024-03-15
     */
    @GetMapping("/user/{userId}/count")
    public ResponseEntity<Map<String, Long>> countActiveActivities(
            @PathVariable Long userId,
            @RequestParam(required = false) String date) {
        
        LocalDate targetDate = date != null ? LocalDate.parse(date) : LocalDate.now();
        long count = activityService.countActiveActivities(userId, targetDate);
        return ResponseEntity.ok(Map.of("count", count));
    }

    /**
     * Verificar si hay conflictos de horarios
     * POST /api/activities/user/{userId}/check-conflict
     */
    @PostMapping("/user/{userId}/check-conflict")
    public ResponseEntity<Map<String, Boolean>> checkScheduleConflict(
            @PathVariable Long userId,
            @RequestBody Activity activity) {
        boolean hasConflict = activityService.hasScheduleConflict(activity, userId);
        return ResponseEntity.ok(Map.of("hasConflict", hasConflict));
    }

    /**
     * Obtener horarios de inicio de una actividad
     * GET /api/activities/{id}/start-times
     */
    @GetMapping("/{id}/start-times")
    public ResponseEntity<?> getStartTimes(@PathVariable Long id) {
        return activityService.getActivityById(id)
                .map(activity -> {
                    List<LocalTime> startTimes = activityService.getStartTimesAsList(activity);
                    return ResponseEntity.ok(startTimes);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Obtener horarios de fin de una actividad
     * GET /api/activities/{id}/end-times
     */
    @GetMapping("/{id}/end-times")
    public ResponseEntity<?> getEndTimes(@PathVariable Long id) {
        return activityService.getActivityById(id)
                .map(activity -> {
                    List<LocalTime> endTimes = activityService.getEndTimesAsList(activity);
                    return ResponseEntity.ok(endTimes);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Obtener días activos de una actividad
     * GET /api/activities/{id}/active-days
     */
    @GetMapping("/{id}/active-days")
    public ResponseEntity<?> getActiveDays(@PathVariable Long id) {
        return activityService.getActivityById(id)
                .map(activity -> {
                    List<DayOfWeek> activeDays = activityService.getActiveDays(activity);
                    return ResponseEntity.ok(activeDays);
                })
                .orElse(ResponseEntity.notFound().build());
    }
}