package com.planiarback.planiar.repository;

import com.planiarback.planiar.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    // Buscar usuario por username
    Optional<User> findByUsername(String username);
    
    // Buscar usuario por email
    Optional<User> findByEmail(String email);
    
    // Verificar si existe un usuario con ese username
    boolean existsByUsername(String username);
    
    // Verificar si existe un usuario con ese email
    boolean existsByEmail(String email);
    
    // Buscar usuarios por username (búsqueda parcial)
    List<User> findByUsernameContainingIgnoreCase(String username);
    
    // Buscar usuarios por email (búsqueda parcial)
    List<User> findByEmailContainingIgnoreCase(String email);

    // Obtener todos los usuarios por tipo
    List<User> findByType(String type);

    // Contar usuarios por tipo
    long countByType(String type);
}