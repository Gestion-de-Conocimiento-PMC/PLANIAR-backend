package com.planiarback.planiar.repository;

import com.planiarback.planiar.model.Class;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ClassRepository extends JpaRepository<Class, Long> {
    
    // Buscar todas las clases de un usuario
    List<Class> findByUserId(Long userId);
    
    // Buscar clase por título
    Optional<Class> findByTitle(String title);
    
    // Buscar clase por título de un usuario específico
    Optional<Class> findByUserIdAndTitle(Long userId, String title);
    
    // Buscar clases que contengan un texto en el título
    List<Class> findByTitleContainingIgnoreCase(String title);
    
    // Buscar clases de un usuario que contengan un texto en el título
    List<Class> findByUserIdAndTitleContainingIgnoreCase(Long userId, String title);
    
    // Verificar si existe una clase con ese título
    boolean existsByTitle(String title);
    
    // Verificar si un usuario tiene una clase con ese título
    boolean existsByUserIdAndTitle(Long userId, String title);
    
    // Buscar clases ordenadas alfabéticamente por título
    List<Class> findByUserIdOrderByTitleAsc(Long userId);
    
    // Buscar clases ordenadas por fecha de inicio
    List<Class> findByUserIdOrderByStartDateAsc(Long userId);
    
    // Contar clases de un usuario
    long countByUserId(Long userId);
    
    // Buscar clases activas (que están en curso en una fecha específica)
    List<Class> findByUserIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            Long userId, LocalDate date1, LocalDate date2);
    
    // Buscar clases por color
    List<Class> findByUserIdAndColor(Long userId, String color);
    
    // Buscar clases en un rango de fechas
    List<Class> findByUserIdAndStartDateBetween(Long userId, LocalDate startDate, LocalDate endDate);
    
    // Buscar clases que terminan después de una fecha
    List<Class> findByUserIdAndEndDateAfter(Long userId, LocalDate date);
    
    // Buscar clases que empiezan antes de una fecha
    List<Class> findByUserIdAndStartDateBefore(Long userId, LocalDate date);
    
    // Buscar clases por profesor
    @Query("SELECT c FROM Class c WHERE c.user.id = :userId AND c.professor LIKE %:professor%")
    List<Class> findByUserIdAndProfessorContaining(@Param("userId") Long userId, @Param("professor") String professor);
    
    // Buscar clases por salón
    @Query("SELECT c FROM Class c WHERE c.user.id = :userId AND c.room LIKE %:room%")
    List<Class> findByUserIdAndRoomContaining(@Param("userId") Long userId, @Param("room") String room);
    
    // Buscar clases que contienen un día específico (0-6)
    @Query("SELECT c FROM Class c WHERE c.user.id = :userId AND c.days LIKE %:day%")
    List<Class> findByUserIdAndDaysContaining(@Param("userId") Long userId, @Param("day") String day);
    
    // Buscar clases activas en una fecha específica
    @Query("SELECT c FROM Class c WHERE c.user.id = :userId " +
           "AND c.startDate <= :date AND c.endDate >= :date")
    List<Class> findByUserIdAndSpecificDate(
            @Param("userId") Long userId, 
            @Param("date") LocalDate date);
    
    // Buscar clases futuras
    List<Class> findByUserIdAndStartDateAfterOrderByStartDateAsc(Long userId, LocalDate currentDate);
    
    // Buscar clases pasadas
    List<Class> findByUserIdAndEndDateBeforeOrderByEndDateDesc(Long userId, LocalDate currentDate);
    
    // Eliminar todas las clases de un usuario
    void deleteByUserId(Long userId);
}