package com.planiarback.planiar.service;

import com.planiarback.planiar.model.Activity;
import com.planiarback.planiar.model.User;
import com.planiarback.planiar.repository.ActivityRepository;
import com.planiarback.planiar.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class ActivityService {

    private final ActivityRepository activityRepository;
    private final UserRepository userRepository;

    public ActivityService(ActivityRepository activityRepository, UserRepository userRepository) {
        this.activityRepository = activityRepository;
        this.userRepository = userRepository;
    }

    /**
     * Crear una nueva actividad
     */
    public Activity createActivity(Activity activity, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con id: " + userId));
        
        activity.setUser(user);
        validateActivity(activity);
        return activityRepository.save(activity);
    }

    /**
     * Obtener una actividad por ID
     */
    public Optional<Activity> getActivityById(Long id) {
        return activityRepository.findById(id);
    }

    /**
     * Obtener todas las actividades de un usuario
     */
    public List<Activity> getAllActivitiesByUser(Long userId) {
        return activityRepository.findByUserId(userId);
    }

    /**
     * Obtener actividades ordenadas por fecha de inicio
     */
    public List<Activity> getActivitiesByUserOrderedByDate(Long userId) {
        return activityRepository.findByUserIdOrderByStartDateAsc(userId);
    }

    /**
     * Obtener actividades activas en una fecha específica
     */
    public List<Activity> getActiveActivitiesOnDate(Long userId, LocalDate date) {
        return activityRepository.findByUserIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                userId, date, date);
    }

    /**
     * Obtener actividades activas hoy
     */
    public List<Activity> getActiveActivitiesToday(Long userId) {
        return getActiveActivitiesOnDate(userId, LocalDate.now());
    }

    /**
     * Obtener actividades para un día de la semana específico
     */
    public List<Activity> getActivitiesByDayOfWeek(Long userId, DayOfWeek dayOfWeek) {
        String dayIndex = String.valueOf(dayOfWeek.getValue() % 7);
        return activityRepository.findByUserIdAndDaysContaining(userId, dayIndex);
    }

    /**
     * Obtener actividades activas para hoy y el día de la semana actual
     */
    public List<Activity> getTodaySchedule(Long userId) {
        LocalDate today = LocalDate.now();
        DayOfWeek dayOfWeek = today.getDayOfWeek();
        String dayIndex = String.valueOf(dayOfWeek.getValue() % 7);
        
        return activityRepository.findActiveActivitiesByUserAndDateAndDay(userId, today, dayIndex);
    }

    /**
     * Obtener actividades por color
     */
    public List<Activity> getActivitiesByColor(Long userId, String color) {
        return activityRepository.findByUserIdAndColor(userId, color);
    }

    /**
     * Obtener actividades en un rango de fechas
     */
    public List<Activity> getActivitiesInRange(Long userId, LocalDate startDate, LocalDate endDate) {
        return activityRepository.findOverlappingActivities(userId, startDate, endDate);
    }

    /**
     * Buscar actividades por título
     */
    public List<Activity> searchActivitiesByTitle(Long userId, String title) {
        return activityRepository.findByUserIdAndTitleContainingIgnoreCase(userId, title);
    }

    /**
     * Obtener actividades futuras
     */
    public List<Activity> getFutureActivities(Long userId) {
        return activityRepository.findByUserIdAndStartDateAfterOrderByStartDateAsc(userId, LocalDate.now());
    }

    /**
     * Obtener actividades pasadas
     */
    public List<Activity> getPastActivities(Long userId) {
        return activityRepository.findByUserIdAndEndDateBeforeOrderByEndDateDesc(userId, LocalDate.now());
    }

    /**
     * Obtener actividades de un usuario en una fecha específica
     */
    public List<Activity> getActivitiesByUserAndDate(Long userId, LocalDate date) {
        return activityRepository.findByUserIdAndSpecificDate(userId, date);
    }

    /**
     * Actualizar una actividad
     */
    public Activity updateActivity(Long id, Activity activityDetails) {
        Activity activity = activityRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Actividad no encontrada con id: " + id));

        activity.setTitle(activityDetails.getTitle());
        activity.setDays(activityDetails.getDays());
        activity.setStartTimes(activityDetails.getStartTimes());
        activity.setEndTimes(activityDetails.getEndTimes());
        activity.setStartDate(activityDetails.getStartDate());
        activity.setEndDate(activityDetails.getEndDate());
        activity.setDescription(activityDetails.getDescription());
        activity.setColor(activityDetails.getColor());

        validateActivity(activity);
        return activityRepository.save(activity);
    }

    /**
     * Eliminar una actividad
     */
    public void deleteActivity(Long id) {
        Activity activity = activityRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Actividad no encontrada con id: " + id));
        activityRepository.delete(activity);
    }

    /**
     * Eliminar todas las actividades de un usuario
     */
    public void deleteAllUserActivities(Long userId) {
        activityRepository.deleteByUserId(userId);
    }

    /**
     * Contar actividades activas
     */
    public long countActiveActivities(Long userId, LocalDate date) {
        return activityRepository.countActiveActivitiesByUser(userId, date);
    }

    /**
     * Verificar si una actividad está activa en una fecha
     */
    public boolean isActivityActiveOnDate(Activity activity, LocalDate date) {
        return !date.isBefore(activity.getStartDate()) && !date.isAfter(activity.getEndDate());
    }

    /**
     * Verificar si una actividad ocurre en un día de la semana específico
     */
    public boolean isActivityOnDayOfWeek(Activity activity, DayOfWeek dayOfWeek) {
        if (activity.getDays() == null || activity.getDays().isEmpty()) {
            return false;
        }
        String[] days = activity.getDays().split(",");
        
        int index = dayOfWeek.getValue() % 7;
        if (index < days.length) {
            return "1".equals(days[index].trim());
        }
        return false;
    }

    /**
     * Obtener lista de horarios de inicio como LocalTime
     */
    public List<LocalTime> getStartTimesAsList(Activity activity) {
        if (activity.getStartTimes() == null || activity.getStartTimes().isEmpty()) {
            return List.of();
        }
        return Arrays.stream(activity.getStartTimes().split(","))
                .map(String::trim)
                .map(LocalTime::parse)
                .collect(Collectors.toList());
    }

    /**
     * Obtener lista de horarios de fin como LocalTime
     */
    public List<LocalTime> getEndTimesAsList(Activity activity) {
        if (activity.getEndTimes() == null || activity.getEndTimes().isEmpty()) {
            return List.of();
        }
        return Arrays.stream(activity.getEndTimes().split(","))
                .map(String::trim)
                .map(LocalTime::parse)
                .collect(Collectors.toList());
    }

    /**
     * Obtener días activos como lista de DayOfWeek
     */
    public List<DayOfWeek> getActiveDays(Activity activity) {
        if (activity.getDays() == null || activity.getDays().isEmpty()) {
            return List.of();
        }
        
        String[] days = activity.getDays().split(",");
        return Arrays.stream(days)
                .map(String::trim)
                .map(Integer::parseInt)
                .filter(day -> day == 1)
                .map(day -> {
                    int index = Arrays.asList(days).indexOf(String.valueOf(day));
                    return DayOfWeek.of(index == 0 ? 7 : index);
                })
                .collect(Collectors.toList());
    }

    /**
     * Validar datos de la actividad
     */
    private void validateActivity(Activity activity) {
        if (activity.getTitle() == null || activity.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("El título no puede estar vacío");
        }

        if (activity.getStartDate() == null || activity.getEndDate() == null) {
            throw new IllegalArgumentException("Las fechas de inicio y fin son obligatorias");
        }

        if (activity.getEndDate().isBefore(activity.getStartDate())) {
            throw new IllegalArgumentException("La fecha de fin no puede ser anterior a la fecha de inicio");
        }

        if (activity.getDays() != null && !activity.getDays().matches("^[0-1](,[0-1])*$")) {
            throw new IllegalArgumentException("Formato de días inválido. Use: 0,1,1,0,1,0,0");
        }

        if (activity.getStartTimes() != null && activity.getEndTimes() != null) {
            int startCount = activity.getStartTimes().split(",").length;
            int endCount = activity.getEndTimes().split(",").length;
            if (startCount != endCount) {
                throw new IllegalArgumentException("El número de horarios de inicio y fin debe coincidir");
            }
        }
    }

    /**
     * Verificar si hay conflicto de horarios con otras actividades
     */
    public boolean hasScheduleConflict(Activity activity, Long userId) {
        List<Activity> overlappingActivities = activityRepository.findOverlappingActivities(
                userId, activity.getStartDate(), activity.getEndDate());
        
        if (activity.getId() != null) {
            overlappingActivities = overlappingActivities.stream()
                    .filter(a -> !a.getId().equals(activity.getId()))
                    .collect(Collectors.toList());
        }

        return !overlappingActivities.isEmpty();
    }
}