package com.planiarback.planiar.repository;

import com.planiarback.planiar.model.Activity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ActivityRepository extends JpaRepository<Activity, Long> {
    
    // Buscar todas las actividades de un usuario
    List<Activity> findByUserId(Long userId);
    
    // Buscar actividad por título
    Optional<Activity> findByTitle(String title);
    
    // Buscar actividad por título de un usuario específico
    Optional<Activity> findByUserIdAndTitle(Long userId, String title);
    
    // Buscar actividades que contengan un texto en el título
    List<Activity> findByTitleContainingIgnoreCase(String title);
    
    // Buscar actividades de un usuario que contengan un texto en el título
    List<Activity> findByUserIdAndTitleContainingIgnoreCase(Long userId, String title);
    
    // Verificar si existe una actividad con ese título
    boolean existsByTitle(String title);
    
    // Verificar si un usuario tiene una actividad con ese título
    boolean existsByUserIdAndTitle(Long userId, String title);
    
    // Buscar actividades ordenadas alfabéticamente por título
    List<Activity> findByUserIdOrderByTitleAsc(Long userId);
    
    // Buscar actividades ordenadas por fecha de inicio
    List<Activity> findByUserIdOrderByStartDateAsc(Long userId);
    
    // Contar actividades de un usuario
    long countByUserId(Long userId);
    
    // Buscar actividades activas (que están en curso en una fecha específica)
    List<Activity> findByUserIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            Long userId, LocalDate date1, LocalDate date2);
    
    // Buscar actividades por color
    List<Activity> findByUserIdAndColor(Long userId, String color);
    
    // Buscar actividades en un rango de fechas
    List<Activity> findByUserIdAndStartDateBetween(Long userId, LocalDate startDate, LocalDate endDate);
    
    // Buscar actividades que terminan después de una fecha
    List<Activity> findByUserIdAndEndDateAfter(Long userId, LocalDate date);
    
    // Buscar actividades que empiezan antes de una fecha
    List<Activity> findByUserIdAndStartDateBefore(Long userId, LocalDate date);
    
    // Buscar actividades que contienen un día específico (0-6)
    @Query("SELECT a FROM Activity a WHERE a.user.id = :userId AND a.days LIKE %:day%")
    List<Activity> findByUserIdAndDaysContaining(@Param("userId") Long userId, @Param("day") String day);
    
    // Buscar actividades activas en una fecha específica que tengan un día particular
    @Query("SELECT a FROM Activity a WHERE a.user.id = :userId " +
           "AND a.startDate <= :date AND a.endDate >= :date " +
           "AND a.days LIKE %:day%")
    List<Activity> findActiveActivitiesByUserAndDateAndDay(
            @Param("userId") Long userId, 
            @Param("date") LocalDate date, 
            @Param("day") String day);
    
    // Contar actividades activas de un usuario
    @Query("SELECT COUNT(a) FROM Activity a WHERE a.user.id = :userId " +
           "AND a.startDate <= :date AND a.endDate >= :date")
    long countActiveActivitiesByUser(@Param("userId") Long userId, @Param("date") LocalDate date);
    
    // Buscar actividades que se solapan con un rango de fechas
    @Query("SELECT a FROM Activity a WHERE a.user.id = :userId " +
           "AND a.startDate <= :endDate AND a.endDate >= :startDate")
    List<Activity> findOverlappingActivities(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
    
    // Buscar todas las actividades futuras
    List<Activity> findByUserIdAndStartDateAfterOrderByStartDateAsc(Long userId, LocalDate currentDate);
    
    // Buscar todas las actividades pasadas
    List<Activity> findByUserIdAndEndDateBeforeOrderByEndDateDesc(Long userId, LocalDate currentDate);
    
    // Buscar actividades de un usuario en una fecha específica
    @Query("SELECT a FROM Activity a WHERE a.user.id = :userId " +
           "AND a.startDate <= :date AND a.endDate >= :date")
    List<Activity> findByUserIdAndSpecificDate(
            @Param("userId") Long userId, 
            @Param("date") LocalDate date);
    
    // Eliminar todas las actividades de un usuario
    void deleteByUserId(Long userId);
}