package com.planiarback.planiar.repository;

import com.planiarback.planiar.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    
    // Buscar todas las tareas de un usuario
    List<Task> findByUserId(Long userId);
    
    // Buscar tareas de un usuario por título
    Optional<Task> findByUserIdAndTitle(Long userId, String title);
    
    // Buscar tareas que contengan un texto en el título
    List<Task> findByUserIdAndTitleContainingIgnoreCase(Long userId, String title);
    
    // Verificar si existe una tarea con ese título para el usuario
    boolean existsByUserIdAndTitle(Long userId, String title);
    
    // Buscar tareas ordenadas por fecha
    List<Task> findByUserIdOrderByDateAsc(Long userId);
    
    // Buscar tareas ordenadas por prioridad
    List<Task> findByUserIdOrderByPriorityAsc(Long userId);
    
    // Contar tareas de un usuario
    long countByUserId(Long userId);
    
    // Buscar tareas de una clase específica
    List<Task> findByUserIdAndClassId(Long userId, Long classId);
    
    // Buscar tareas de un usuario por estado
    List<Task> findByUserIdAndState(Long userId, String state);
    
    // Buscar tareas de un usuario por prioridad
    List<Task> findByUserIdAndPriority(Long userId, String priority);
    
    // Buscar tareas de un usuario por tipo
    List<Task> findByUserIdAndType(Long userId, String type);
    
    // Buscar tareas de una fecha específica
    List<Task> findByUserIdAndDate(Long userId, LocalDate date);
    
    // Buscar tareas en un rango de fechas
    List<Task> findByUserIdAndDateBetween(Long userId, LocalDate startDate, LocalDate endDate);
    
    // Buscar tareas vencidas (con fecha anterior a hoy)
    List<Task> findByUserIdAndDateBefore(Long userId, LocalDate currentDate);
    
    // Buscar tareas futuras
    List<Task> findByUserIdAndDateAfterOrderByDateAsc(Long userId, LocalDate currentDate);
    
    // Buscar tareas sin clase asignada
    List<Task> findByUserIdAndClassIdIsNull(Long userId);
    
    // Buscar tareas con clase asignada
    List<Task> findByUserIdAndClassIdIsNotNull(Long userId);
    
    // Buscar tareas sin fecha
    List<Task> findByUserIdAndDateIsNull(Long userId);
    
    // Buscar tareas sin descripción
    List<Task> findByUserIdAndDescriptionIsNull(Long userId);
    
    // Contar tareas de un usuario por estado
    long countByUserIdAndState(Long userId, String state);
    
    // Contar tareas de un usuario por prioridad
    long countByUserIdAndPriority(Long userId, String priority);
    
    // Eliminar todas las tareas de un usuario
    void deleteByUserId(Long userId);
}