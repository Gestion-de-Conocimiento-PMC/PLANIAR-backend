package com.planiarback.planiar.service;

import com.planiarback.planiar.model.User;
import com.planiarback.planiar.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
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
        
        if (user.getAdmin() == null) {
            user.setAdmin(false);
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
        return userRepository.findByAdminTrue();
    }

    /**
     * Obtener todos los usuarios no administradores
     */
    @Transactional(readOnly = true)
    public List<User> getAllNonAdminUsers() {
        return userRepository.findByAdminFalse();
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
        
        if (userDetails.getAdmin() != null) {
            user.setAdmin(userDetails.getAdmin());
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
     * Cambiar estado de administrador
     */
    public User toggleAdminStatus(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con id: " + id));

        user.setAdmin(!user.getAdmin());
        return userRepository.save(user);
    }

    /**
     * Establecer como administrador
     */
    public User setAsAdmin(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con id: " + id));

        user.setAdmin(true);
        return userRepository.save(user);
    }

    /**
     * Remover como administrador
     */
    public User removeAsAdmin(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con id: " + id));

        user.setAdmin(false);
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
        return userRepository.countByAdminTrue();
    }

    /**
     * Contar usuarios no administradores
     */
    @Transactional(readOnly = true)
    public long countNonAdminUsers() {
        return userRepository.countByAdminFalse();
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
            return user;
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