package com.planiarback.planiar.service;

import com.planiarback.planiar.model.Class;
import com.planiarback.planiar.model.User;
import com.planiarback.planiar.repository.ClassRepository;
import com.planiarback.planiar.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class ClassService {

    private final ClassRepository classRepository;
    private final UserRepository userRepository;

    public ClassService(ClassRepository classRepository, UserRepository userRepository) {
        this.classRepository = classRepository;
        this.userRepository = userRepository;
    }

    /**
     * Crear una nueva clase
     */
    public Class createClass(Class classEntity, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con id: " + userId));
        
        classEntity.setUser(user);
        validateClass(classEntity);
        return classRepository.save(classEntity);
    }

    /**
     * Obtener todas las clases
     */
    @Transactional(readOnly = true)
    public List<Class> getAllClasses() {
        return classRepository.findAll();
    }

    /**
     * Obtener todas las clases de un usuario
     */
    @Transactional(readOnly = true)
    public List<Class> getAllClassesByUser(Long userId) {
        return classRepository.findByUserId(userId);
    }

    /**
     * Obtener clases ordenadas alfabéticamente
     */
    @Transactional(readOnly = true)
    public List<Class> getClassesOrderedByTitle(Long userId) {
        return classRepository.findByUserIdOrderByTitleAsc(userId);
    }

    /**
     * Obtener clases ordenadas por fecha de inicio
     */
    @Transactional(readOnly = true)
    public List<Class> getClassesOrderedByDate(Long userId) {
        return classRepository.findByUserIdOrderByStartDateAsc(userId);
    }

    /**
     * Obtener una clase por ID
     */
    @Transactional(readOnly = true)
    public Optional<Class> getClassById(Long id) {
        return classRepository.findById(id);
    }

    /**
     * Obtener clase por título
     */
    @Transactional(readOnly = true)
    public Optional<Class> getClassByTitle(String title) {
        return classRepository.findByTitle(title);
    }

    /**
     * Obtener clase por título de un usuario
     */
    @Transactional(readOnly = true)
    public Optional<Class> getClassByTitleAndUser(Long userId, String title) {
        return classRepository.findByUserIdAndTitle(userId, title);
    }

    /**
     * Buscar clases por título
     */
    @Transactional(readOnly = true)
    public List<Class> searchClassesByTitle(String title) {
        return classRepository.findByTitleContainingIgnoreCase(title);
    }

    /**
     * Buscar clases de un usuario por título
     */
    @Transactional(readOnly = true)
    public List<Class> searchClassesByTitleAndUser(Long userId, String title) {
        return classRepository.findByUserIdAndTitleContainingIgnoreCase(userId, title);
    }

    /**
     * Obtener clases activas en una fecha específica
     */
    @Transactional(readOnly = true)
    public List<Class> getActiveClassesOnDate(Long userId, LocalDate date) {
        return classRepository.findByUserIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                userId, date, date);
    }

    /**
     * Obtener clases activas hoy
     */
    @Transactional(readOnly = true)
    public List<Class> getActiveClassesToday(Long userId) {
        return getActiveClassesOnDate(userId, LocalDate.now());
    }

    /**
     * Obtener clases por color
     */
    @Transactional(readOnly = true)
    public List<Class> getClassesByColor(Long userId, String color) {
        return classRepository.findByUserIdAndColor(userId, color);
    }

    /**
     * Obtener clases en un rango de fechas
     */
    @Transactional(readOnly = true)
    public List<Class> getClassesInRange(Long userId, LocalDate startDate, LocalDate endDate) {
        return classRepository.findByUserIdAndStartDateBetween(userId, startDate, endDate);
    }

    /**
     * Obtener clases futuras
     */
    @Transactional(readOnly = true)
    public List<Class> getFutureClasses(Long userId) {
        return classRepository.findByUserIdAndStartDateAfterOrderByStartDateAsc(userId, LocalDate.now());
    }

    /**
     * Obtener clases pasadas
     */
    @Transactional(readOnly = true)
    public List<Class> getPastClasses(Long userId) {
        return classRepository.findByUserIdAndEndDateBeforeOrderByEndDateDesc(userId, LocalDate.now());
    }

    /**
     * Buscar clases por profesor
     */
    @Transactional(readOnly = true)
    public List<Class> searchClassesByProfessor(Long userId, String professor) {
        return classRepository.findByUserIdAndProfessorContaining(userId, professor);
    }

    /**
     * Buscar clases por salón
     */
    @Transactional(readOnly = true)
    public List<Class> searchClassesByRoom(Long userId, String room) {
        return classRepository.findByUserIdAndRoomContaining(userId, room);
    }

    /**
     * Obtener clases para un día de la semana específico
     */
    @Transactional(readOnly = true)
    public List<Class> getClassesByDayOfWeek(Long userId, int dayOfWeek) {
        String dayIndex = String.valueOf(dayOfWeek);
        return classRepository.findByUserIdAndDaysContaining(userId, dayIndex);
    }

    /**
     * Obtener clases de un usuario en una fecha específica
     */
    @Transactional(readOnly = true)
    public List<Class> getClassesByUserAndDate(Long userId, LocalDate date) {
        return classRepository.findByUserIdAndSpecificDate(userId, date);
    }

    /**
     * Actualizar una clase
     */
    public Class updateClass(Long id, Class classDetails) {
        Class classEntity = classRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Clase no encontrada con id: " + id));

        Long userId = classEntity.getUser().getId();

        if (!classEntity.getTitle().equals(classDetails.getTitle()) &&
            classRepository.existsByUserIdAndTitle(userId, classDetails.getTitle())) {
            throw new IllegalArgumentException("Ya existe una clase con este título");
        }

        classEntity.setTitle(classDetails.getTitle());
        classEntity.setDays(classDetails.getDays());
        classEntity.setStartTimes(classDetails.getStartTimes());
        classEntity.setEndTimes(classDetails.getEndTimes());
        classEntity.setStartDate(classDetails.getStartDate());
        classEntity.setEndDate(classDetails.getEndDate());
        classEntity.setProfessor(classDetails.getProfessor());
        classEntity.setRoom(classDetails.getRoom());
        classEntity.setColor(classDetails.getColor());

        validateClass(classEntity);
        return classRepository.save(classEntity);
    }

    /**
     * Eliminar una clase
     */
    public void deleteClass(Long id) {
        if (!classRepository.existsById(id)) {
            throw new IllegalArgumentException("Clase no encontrada con id: " + id);
        }
        classRepository.deleteById(id);
    }

    /**
     * Eliminar todas las clases de un usuario
     */
    public void deleteAllUserClasses(Long userId) {
        classRepository.deleteByUserId(userId);
    }

    /**
     * Contar clases de un usuario
     */
    @Transactional(readOnly = true)
    public long countUserClasses(Long userId) {
        return classRepository.countByUserId(userId);
    }

    /**
     * Verificar si existe una clase con ese título
     */
    @Transactional(readOnly = true)
    public boolean existsByTitle(String title) {
        return classRepository.existsByTitle(title);
    }

    /**
     * Verificar si un usuario tiene una clase con ese título
     */
    @Transactional(readOnly = true)
    public boolean existsByUserIdAndTitle(Long userId, String title) {
        return classRepository.existsByUserIdAndTitle(userId, title);
    }

    /**
     * Obtener lista de horarios de inicio como LocalTime
     */
    public List<LocalTime> getStartTimesAsList(Class classEntity) {
        if (classEntity.getStartTimes() == null || classEntity.getStartTimes().isEmpty()) {
            return List.of();
        }
        return Arrays.stream(classEntity.getStartTimes().split(","))
                .map(String::trim)
                .map(LocalTime::parse)
                .collect(Collectors.toList());
    }

    /**
     * Obtener lista de horarios de fin como LocalTime
     */
    public List<LocalTime> getEndTimesAsList(Class classEntity) {
        if (classEntity.getEndTimes() == null || classEntity.getEndTimes().isEmpty()) {
            return List.of();
        }
        return Arrays.stream(classEntity.getEndTimes().split(","))
                .map(String::trim)
                .map(LocalTime::parse)
                .collect(Collectors.toList());
    }

    /**
     * Obtener lista de profesores
     */
    public List<String> getProfessorsAsList(Class classEntity) {
        if (classEntity.getProfessor() == null || classEntity.getProfessor().isEmpty()) {
            return List.of();
        }
        return Arrays.stream(classEntity.getProfessor().split(","))
                .map(String::trim)
                .collect(Collectors.toList());
    }

    /**
     * Obtener lista de salones
     */
    public List<String> getRoomsAsList(Class classEntity) {
        if (classEntity.getRoom() == null || classEntity.getRoom().isEmpty()) {
            return List.of();
        }
        return Arrays.stream(classEntity.getRoom().split(","))
                .map(String::trim)
                .collect(Collectors.toList());
    }

    /**
     * Validar datos de la clase
     */
    private void validateClass(Class classEntity) {
        if (classEntity.getTitle() == null || classEntity.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("El título no puede estar vacío");
        }

        if (classEntity.getStartDate() == null || classEntity.getEndDate() == null) {
            throw new IllegalArgumentException("Las fechas de inicio y fin son obligatorias");
        }

        if (classEntity.getEndDate().isBefore(classEntity.getStartDate())) {
            throw new IllegalArgumentException("La fecha de fin no puede ser anterior a la fecha de inicio");
        }

        if (classEntity.getDays() != null && !classEntity.getDays().matches("^[0-1](,[0-1])*$")) {
            throw new IllegalArgumentException("Formato de días inválido. Use: 0,1,1,0,1,0,0");
        }

        if (classEntity.getStartTimes() != null && classEntity.getEndTimes() != null) {
            int startCount = classEntity.getStartTimes().split(",").length;
            int endCount = classEntity.getEndTimes().split(",").length;
            if (startCount != endCount) {
                throw new IllegalArgumentException("El número de horarios de inicio y fin debe coincidir");
            }
        }

        if (classEntity.getProfessor() != null && classEntity.getRoom() != null) {
            int professorCount = classEntity.getProfessor().split(",").length;
            int roomCount = classEntity.getRoom().split(",").length;
            if (professorCount != roomCount) {
                throw new IllegalArgumentException("El número de profesores y salones debe coincidir");
            }
        }
    }
}