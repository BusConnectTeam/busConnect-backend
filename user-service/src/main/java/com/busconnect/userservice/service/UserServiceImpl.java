package com.busconnect.userservice.service;

import com.busconnect.userservice.dto.request.CreateUserRequest;
import com.busconnect.userservice.dto.request.UpdateUserRequest;
import com.busconnect.userservice.dto.response.UserResponse;
import com.busconnect.userservice.exception.EmailAlreadyExistsException;
import com.busconnect.userservice.exception.UserNotFoundException;
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
 * Implementation reactive of UserService.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    /**
     * Create a new user.
     * Note: Email is logging in DEBUG level to be PII (GDPR compliance).
     *
     * @param request Dates of the new user.
     * @return Mono with the response of the new user.
     */
    @Override
    public Mono<UserResponse> createUser(CreateUserRequest request) {
        return userRepository.existsByEmail(request.getEmail())
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(new EmailAlreadyExistsException("email.already.exists"));
                    }
                    User user = new User();
                    user.setEmail(request.getEmail());
                    user.setFirstName(request.getFirstName());
                    user.setLastName(request.getLastName());
                    user.setPhone(request.getPhone());
                    user.setRole(request.getRole());
                    user.setCreatedAt(LocalDateTime.now());
                    user.setUpdatedAt(LocalDateTime.now());
                    user.setActive(true);

                    return userRepository.save(user)
                            .doOnSuccess(savedUser ->
                                    log.debug("User created successfully with ID: {}", savedUser.getId()))
                            .map(this::toResponse);
                });
    }

    /**
     * Gets a user by ID.
     *
     * @param id User ID.
     * @return Mono with the response of the user.
     */
    @Override
    public Mono<UserResponse> getUserById(Long id) {
        log.debug("Fetching user by ID: {}", id);

        return userRepository.findById(id)
                .map(this::toResponse)
                .switchIfEmpty(Mono.error(new UserNotFoundException("user.not.found")));
    }

    /**
     * Gets a user by email.
     * Note: Email is logging in DEBUG level to be PII (GDPR compliance).
     *
     * @param email User email.
     * @return Mono with the response of the user.
     */
    @Override
    public Mono<UserResponse> getUserByEmail(String email) {
        log.debug("Fetching user by email: {}", email);

        return userRepository.findByEmail(email)
                .map(this::toResponse)
                .switchIfEmpty(Mono.error(new UserNotFoundException("user.not.found")));
    }

    /**
     * Updates a user.
     *
     * @param id      User ID to update.
     * @param request Dates to update.
     * @return Mono with the response of the updated user.
     */
    @Override
    public Mono<UserResponse> updateUser(Long id, UpdateUserRequest request) {
        log.debug("Updating user with ID: {}", id);

        return userRepository.findById(id)
                .flatMap(existingUser -> {
                    if (request.getEmail() != null) existingUser.setEmail(request.getEmail());
                    if (request.getFirstName() != null) existingUser.setFirstName(request.getFirstName());
                    if (request.getLastName() != null) existingUser.setLastName(request.getLastName());
                    if (request.getPhone() != null) existingUser.setPhone(request.getPhone());
                    if (request.getRole() != null) existingUser.setRole(request.getRole());
                    existingUser.setUpdatedAt(LocalDateTime.now());
                    return userRepository.save(existingUser)
                            .map(this::toResponse);
                })
                .switchIfEmpty(Mono.error(new UserNotFoundException("user.not.found")));
    }

    /**
     * Get all users.
     *
     * @return Flux with the response of all users.
     */
    @Override
    public Flux<UserResponse> getAllUsers() {
        log.debug("Fetching all users");

        return userRepository.findAll()
                .map(this::toResponse);
    }

    /**
     * Soft deletes a user.
     *
     * @param id User ID to delete.
     * @return Mono empty.
     */
    @Override
    public Mono<Void> softDeleteUser(Long id) {
        log.debug("Soft deleting user with ID: {}", id);

        return userRepository.findById(id)
                .flatMap(user -> {
                    user.setActive(false);
                    user.setUpdatedAt(LocalDateTime.now());
                    return userRepository.save(user);
                })
                .switchIfEmpty(Mono.error(new UserNotFoundException("user.not.found")))
                .then();
    }

    /**
     * Restore a deleted user.
     *
     * @param id User ID to restore.
     * @return Mono with the response of the restored user.
     */
    @Override
    public Mono<UserResponse> restoreUser(Long id) {
        log.info("Restoring user with ID: {}", id);

        return userRepository.findById(id)
                .flatMap(user -> {
                    user.setActive(true);
                    user.setUpdatedAt(LocalDateTime.now());
                    return userRepository.save(user)
                            .map(this::toResponse);
                })
                .switchIfEmpty(Mono.error(new UserNotFoundException("user.not.found")));
    }

    /**
     * Eliminate an User Permanently.
     *
     * @param id User ID to delete.
     * @return Mono empty.
     */
    @Override
    public Mono<Void> deleteUserPermanently(Long id) {
        log.warn("PERMANENTLY deleting user with ID: {}", id);

        return userRepository.deleteById(id);
    }

    /**
     * Convert an object User to a UserResponse.
     *
     * @param user User to convert.
     * @return Answer response.
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
