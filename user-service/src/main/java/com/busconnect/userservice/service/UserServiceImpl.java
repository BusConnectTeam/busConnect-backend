package com.busconnect.userservice.service;

import com.busconnect.userservice.dto.request.CreateUserRequest;
import com.busconnect.userservice.dto.request.UpdateUserRequest;
import com.busconnect.userservice.dto.response.UserResponse;
import com.busconnect.userservice.model.User;
import com.busconnect.userservice.repository.UserRepository;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

/**
 * Implementación reactiva de UserService.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    /**
     * Crea un nuevo usuario.
     *
     * @param request Datos del usuario a crear.
     * @return Mono con la respuesta del usuario creado.
     */
    @Override
    public Mono<UserResponse> createUser(CreateUserRequest request) {
        return userRepository.existsByEmail(request.getEmail())
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(new RuntimeException("El email ya está registrado."));
                    }
                    User user = new User();
                    user.setEmail(request.getEmail());
                    user.setPasswordHash(request.getPassword()); // En la siguiente fase se encriptará
                    user.setFirstName(request.getFirstName());
                    user.setLastName(request.getLastName());
                    user.setPhone(request.getPhone());
                    user.setRole(request.getRole());
                    user.setCreatedAt(LocalDateTime.now());
                    user.setUpdatedAt(LocalDateTime.now());
                    user.setActive(true);

                    return userRepository.save(user)
                            .map(this::toResponse);
                });
    }

    /**
     * Obtiene un usuario por su ID.
     *
     * @param id ID del usuario.
     * @return Mono con la respuesta del usuario.
     */
    @Override
    public Mono<UserResponse> getUserById(Long id) {
        return userRepository.findById(id)
                .map(this::toResponse)
                .switchIfEmpty(Mono.error(new RuntimeException("Usuario no encontrado")));
    }

    /**
     * Obtiene un usuario por su email.
     *
     * @param email Email del usuario.
     * @return Mono con la respuesta del usuario.
     */
    @Override
    public Mono<UserResponse> getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(this::toResponse)
                .switchIfEmpty(Mono.error(new RuntimeException("Usuario no encontrado")));
    }

    /**
     * Actualiza un usuario existente.
     *
     * @param id      ID del usuario a actualizar.
     * @param request Datos para actualizar el usuario.
     * @return Mono con la respuesta del usuario actualizado.
     */
    @Override
    public Mono<UserResponse> updateUser(Long id, UpdateUserRequest request) {
        return userRepository.findById(id)
                .flatMap(existingUser -> {
                    if (request.getEmail() != null) existingUser.setEmail(request.getEmail());
                    if (request.getPassword() != null) existingUser.setPasswordHash(request.getPassword());
                    if (request.getFirstName() != null) existingUser.setFirstName(request.getFirstName());
                    if (request.getLastName() != null) existingUser.setLastName(request.getLastName());
                    if (request.getPhone() != null) existingUser.setPhone(request.getPhone());
                    if (request.getRole() != null) existingUser.setRole(request.getRole());
                    existingUser.setUpdatedAt(LocalDateTime.now());
                    return userRepository.save(existingUser)
                            .map(this::toResponse);
                })
                .switchIfEmpty(Mono.error(new RuntimeException("Usuario no encontrado")));
    }

    /**
     * Obtiene todos los usuarios.
     *
     * @return Flux con las respuestas de todos los usuarios.
     */
    @Override
    public Flux<UserResponse> getAllUsers() {
        return userRepository.findAll()
                .map(this::toResponse);
    }

    /**
     * Elimina un usuario de forma lógica.
     *
     * @param id ID del usuario a eliminar.
     * @return Mono vacío.
     */
    @Override
    public Mono<Void> softDeleteUser(Long id) {
        return userRepository.findById(id)
                .flatMap(user -> {
                    user.setActive(false);
                    user.setUpdatedAt(LocalDateTime.now());
                    return userRepository.save(user);
                })
                .switchIfEmpty(Mono.error(new RuntimeException("Usuario no encontrado")))
                .then();
    }

    /**
     * Restaura un usuario eliminado de forma lógica.
     *
     * @param id ID del usuario a restaurar.
     * @return Mono con la respuesta del usuario restaurado.
     */
    @Override
    public Mono<UserResponse> restoreUser(Long id) {
        return userRepository.findById(id)
                .flatMap(user -> {
                    user.setActive(true);
                    user.setUpdatedAt(LocalDateTime.now());
                    return userRepository.save(user)
                            .map(this::toResponse);
                })
                .switchIfEmpty(Mono.error(new RuntimeException("Usuario no encontrado")));
    }

    /**
     * Elimina un usuario de forma permanente.
     *
     * @param id ID del usuario a eliminar.
     * @return Mono vacío.
     */
    @Override
    public Mono<Void> deleteUserPermanently(Long id) {
        return userRepository.deleteById(id);
    }

    /**
     * Convierte un objeto User a UserResponse.
     *
     * @param user Usuario a convertir.
     * @return Respuesta del usuario.
     */
    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhone(),
                user.getRole(),
                user.isActive(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
