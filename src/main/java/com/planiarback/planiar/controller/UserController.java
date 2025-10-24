package com.planiarback.planiar.controller;

import com.planiarback.planiar.model.User;
import com.planiarback.planiar.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Login de usuario
     * POST /api/users/login
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> loginRequest) {
        try {
            String username = loginRequest.get("username");
            String password = loginRequest.get("password");

            if (username == null || username.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "El nombre de usuario es requerido"));
            }

            if (password == null || password.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "La contraseña es requerida"));
            }

            var user = userService.login(username, password);

            if (user.isPresent()) {
                return ResponseEntity.ok(user.get());
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Credenciales inválidas. Usuario o contraseña incorrectos"));
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al procesar el login"));
        }
    }

    /**
     * Crear un nuevo usuario
     * POST /api/users
     */
    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody User user) {
        try {
            User createdUser = userService.createUser(user);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Obtener todos los usuarios
     * GET /api/users
     */
    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    /**
     * Obtener un usuario por ID
     * GET /api/users/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        return userService.getUserById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Obtener usuario por username
     * GET /api/users/username/{username}
     */
    @GetMapping("/username/{username}")
    public ResponseEntity<?> getUserByUsername(@PathVariable String username) {
        return userService.getUserByUsername(username)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Obtener usuario por email
     * GET /api/users/email/{email}
     */
    @GetMapping("/email/{email}")
    public ResponseEntity<?> getUserByEmail(@PathVariable String email) {
        return userService.getUserByEmail(email)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Buscar usuarios por username
     * GET /api/users/search/username?username=juan
     */
    @GetMapping("/search/username")
    public ResponseEntity<List<User>> searchUsersByUsername(@RequestParam String username) {
        List<User> users = userService.searchUsersByUsername(username);
        return ResponseEntity.ok(users);
    }

    /**
     * Buscar usuarios por email
     * GET /api/users/search/email?email=example
     */
    @GetMapping("/search/email")
    public ResponseEntity<List<User>> searchUsersByEmail(@RequestParam String email) {
        List<User> users = userService.searchUsersByEmail(email);
        return ResponseEntity.ok(users);
    }

    /**
     * Obtener todos los usuarios administradores
     * GET /api/users/admin
     */
    @GetMapping("/admin")
    public ResponseEntity<List<User>> getAllAdminUsers() {
        List<User> users = userService.getAllAdminUsers();
        return ResponseEntity.ok(users);
    }

    /**
     * Obtener todos los usuarios no administradores
     * GET /api/users/non-admin
     */
    @GetMapping("/non-admin")
    public ResponseEntity<List<User>> getAllNonAdminUsers() {
        List<User> users = userService.getAllNonAdminUsers();
        return ResponseEntity.ok(users);
    }

    /**
     * Verificar si existe un usuario con ese username
     * GET /api/users/exists/username?username=juan
     */
    @GetMapping("/exists/username")
    public ResponseEntity<Map<String, Boolean>> checkUsernameExists(@RequestParam String username) {
        boolean exists = userService.existsByUsername(username);
        return ResponseEntity.ok(Map.of("exists", exists));
    }

    /**
     * Verificar si existe un usuario con ese email
     * GET /api/users/exists/email?email=juan@example.com
     */
    @GetMapping("/exists/email")
    public ResponseEntity<Map<String, Boolean>> checkEmailExists(@RequestParam String email) {
        boolean exists = userService.existsByEmail(email);
        return ResponseEntity.ok(Map.of("exists", exists));
    }

    /**
     * Actualizar un usuario completo
     * PUT /api/users/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody User userDetails) {
        try {
            User updatedUser = userService.updateUser(id, userDetails);
            return ResponseEntity.ok(updatedUser);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Actualizar solo el username
     * PATCH /api/users/{id}/username
     */
    @PatchMapping("/{id}/username")
    public ResponseEntity<?> updateUsername(@PathVariable Long id, @RequestBody Map<String, String> request) {
        try {
            String newUsername = request.get("username");
            if (newUsername == null || newUsername.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "El nombre de usuario no puede estar vacío"));
            }
            User updatedUser = userService.updateUsername(id, newUsername);
            return ResponseEntity.ok(updatedUser);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Actualizar solo el email
     * PATCH /api/users/{id}/email
     */
    @PatchMapping("/{id}/email")
    public ResponseEntity<?> updateEmail(@PathVariable Long id, @RequestBody Map<String, String> request) {
        try {
            String newEmail = request.get("email");
            if (newEmail == null || newEmail.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "El email no puede estar vacío"));
            }
            User updatedUser = userService.updateEmail(id, newEmail);
            return ResponseEntity.ok(updatedUser);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Actualizar solo la contraseña
     * PATCH /api/users/{id}/password
     */
    @PatchMapping("/{id}/password")
    public ResponseEntity<?> updatePassword(@PathVariable Long id, @RequestBody Map<String, String> request) {
        try {
            String newPassword = request.get("password");
            if (newPassword == null || newPassword.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "La contraseña no puede estar vacía"));
            }
            User updatedUser = userService.updatePassword(id, newPassword);
            return ResponseEntity.ok(updatedUser);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Cambiar estado de administrador
     * PATCH /api/users/{id}/toggle-admin
     */
    @PatchMapping("/{id}/toggle-admin")
    public ResponseEntity<?> toggleAdminStatus(@PathVariable Long id) {
        try {
            User updatedUser = userService.toggleAdminStatus(id);
            return ResponseEntity.ok(updatedUser);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Establecer como administrador
     * PATCH /api/users/{id}/set-admin
     */
    @PatchMapping("/{id}/set-admin")
    public ResponseEntity<?> setAsAdmin(@PathVariable Long id) {
        try {
            User updatedUser = userService.setAsAdmin(id);
            return ResponseEntity.ok(updatedUser);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Remover como administrador
     * PATCH /api/users/{id}/remove-admin
     */
    @PatchMapping("/{id}/remove-admin")
    public ResponseEntity<?> removeAsAdmin(@PathVariable Long id) {
        try {
            User updatedUser = userService.removeAsAdmin(id);
            return ResponseEntity.ok(updatedUser);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Eliminar un usuario
     * DELETE /api/users/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        try {
            userService.deleteUser(id);
            return ResponseEntity.ok(Map.of("message", "Usuario eliminado exitosamente"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Contar todos los usuarios
     * GET /api/users/count/total
     */
    @GetMapping("/count/total")
    public ResponseEntity<Map<String, Long>> countAllUsers() {
        long count = userService.countAllUsers();
        return ResponseEntity.ok(Map.of("count", count));
    }

    /**
     * Contar usuarios administradores
     * GET /api/users/count/admin
     */
    @GetMapping("/count/admin")
    public ResponseEntity<Map<String, Long>> countAdminUsers() {
        long count = userService.countAdminUsers();
        return ResponseEntity.ok(Map.of("count", count));
    }

    /**
     * Contar usuarios no administradores
     * GET /api/users/count/non-admin
     */
    @GetMapping("/count/non-admin")
    public ResponseEntity<Map<String, Long>> countNonAdminUsers() {
        long count = userService.countNonAdminUsers();
        return ResponseEntity.ok(Map.of("count", count));
    }
}