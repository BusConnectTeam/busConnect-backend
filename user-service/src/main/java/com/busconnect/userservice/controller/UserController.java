package com.busconnect.userservice.controller;

import com.busconnect.userservice.dto.request.CreateUserRequest;
import com.busconnect.userservice.dto.request.UpdateUserRequest;
import com.busconnect.userservice.dto.response.UserResponse;
import com.busconnect.userservice.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Controller for user-related endpoints with Reactive.
 *
 * Logging Strategy:
 * - DEBUG: Routine operations and requests containing PII (emails)
 * - ERROR: Exception handling
 * - Success logs are handled at service layer to avoid duplication
 */
@Slf4j
@Tag(name = "Users", description = "API for user management")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

        private final UserService userService;

        /**
         * Create a new User
         *
         * @param request user data
         * @return Mono with created user response
         */
        @Operation(summary = "Create a new user", description = "Creates a new user in the system")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "201", description = "User created successfully"),
                        @ApiResponse(responseCode = "400", description = "Invalid input data"),
                        @ApiResponse(responseCode = "409", description = "Email already exists")
        })
        @PostMapping
        public Mono<ResponseEntity<UserResponse>> createUser(
                        @Valid @RequestBody CreateUserRequest request) {
                log.debug("Received request to create user with email: {}", request.getEmail());

                return userService.createUser(request)
                                .map(userResponse -> ResponseEntity
                                                .status(HttpStatus.CREATED)
                                                .body(userResponse))
                                .doOnError(error -> log.error("Error creating user: {}", error.getMessage()));
        }

        /**
         * Get user by ID
         *
         * @param id user ID
         * @return Mono with user response
         */
        @Operation(summary = "Get user by ID", description = "Retrieves a user by their ID")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "User found"),
                        @ApiResponse(responseCode = "404", description = "User not found")
        })
        @GetMapping("/{id}")
        public Mono<ResponseEntity<UserResponse>> getUserById(@PathVariable Long id) {
                log.debug("Received request to get user with ID: {}", id);

                return userService.getUserById(id)
                                .map(userResponse -> ResponseEntity.ok(userResponse))
                                .doOnError(error -> log.error("Error retrieving user: {}", error.getMessage()));
        }

        /**
         * Get user by email
         *
         * @param email user email
         * @return Mono with user response
         */
        @Operation(summary = "Get user by email", description = "Retrieves a user by their email")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "User found"),
                        @ApiResponse(responseCode = "404", description = "User not found")
        })
        @GetMapping("/email/{email}")
        public Mono<ResponseEntity<UserResponse>> getUserByEmail(@PathVariable String email) {
                log.debug("Received request to get user with email: {}", email);

                return userService.getUserByEmail(email)
                                .map(userResponse -> ResponseEntity.ok(userResponse))
                                .doOnError(error -> log.error("Error retrieving user: {}", error.getMessage()));
        }

        /**
         * Update user by ID
         *
         * @param id      user ID
         * @param request updated user data
         * @return Mono with updated user response
         */
        @Operation(summary = "Update user by ID", description = "Updates a user by their ID")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "User updated successfully"),
                        @ApiResponse(responseCode = "404", description = "User not found")
        })
        @PutMapping("/{id}")
        public Mono<ResponseEntity<UserResponse>> updateUserById(@PathVariable Long id,
                        @Valid @RequestBody UpdateUserRequest request) {
                log.debug("Received request to update user with ID: {}", id);

                return userService.updateUser(id, request)
                                .map(userResponse -> ResponseEntity.ok(userResponse))
                                .doOnError(error -> log.error("Error updating user: {}", error.getMessage()));
        }

        /**
         * Get all users
         *
         * @return Flux with all user responses
         */
        @Operation(summary = "Get all users", description = "Retrieves all users in the system")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Users retrieved successfully")
        })
        @GetMapping
        public Flux<UserResponse> getAllUsers() {
                log.debug("Received request to get all users");

                return userService.getAllUsers()
                                .doOnError(error -> log.error("Error retrieving users: {}", error.getMessage()));
        }

        /**
         * Soft delete user by ID
         *
         * @param id user ID
         * @return Mono with deleted user response
         */
        @Operation(summary = "Soft delete user by ID", description = "Soft deletes a user by their ID")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "204", description = "User soft deleted successfully"),
                        @ApiResponse(responseCode = "404", description = "User not found")
        })
        @DeleteMapping("/{id}")
        public Mono<ResponseEntity<Void>> softDeleteUserById(@PathVariable Long id) {
                log.debug("Received request to delete user with ID: {}", id);

                return userService.softDeleteUser(id)
                                .then(Mono.just(ResponseEntity.noContent().<Void>build()))
                                .doOnError(error -> log.error("Error soft deleting user: {}", error.getMessage()));
        }

        /**
         * Restore user by ID
         *
         * @param id user ID
         * @return Mono with restored user response
         */
        @Operation(summary = "Restore user by ID", description = "Restores a soft deleted user by their ID")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "User restored successfully"),
                        @ApiResponse(responseCode = "404", description = "User not found")
        })
        @PostMapping("/{id}/restore")
        public Mono<ResponseEntity<UserResponse>> restoreUserById(@PathVariable Long id) {
                log.debug("Received request to restore user with ID: {}", id);

                return userService.restoreUser(id)
                                .map(userResponse -> ResponseEntity.ok(userResponse))
                                .doOnError(error -> log.error("Error restoring user: {}", error.getMessage()));
        }

        /**
         * Delete user permanently by ID
         *
         * @param userId user ID
         * @return Mono with no content
         */
        @Operation(summary = "Permanently delete user by ID", description = "Permanently deletes a user by their ID")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "204", description = "User permanently deleted successfully"),
                        @ApiResponse(responseCode = "404", description = "User not found")
        })
        @DeleteMapping("/{id}/permanent")
        public Mono<ResponseEntity<Object>> permanentlyDeleteUserById(@PathVariable Long id) {
                log.debug("Received request to permanently delete user with ID: {}", id);

                return userService.deleteUserPermanently(id)
                                .then(Mono.just(ResponseEntity.noContent().build()))
                                .doOnError(error -> log.error("Error permanently deleting user: {}",
                                                error.getMessage()));
        }

        /**
         * Get authenticated user details.
         *
         * @param userDetails currently authenticated user details
         * @return Mono with user response
         */
        @Operation(summary = "Get authenticated user", description = "Retrieves details of the currently authenticated user")
        @ApiResponses(value = {
                @ApiResponse(responseCode = "200", description = "User details retrieved successfully"),
                @ApiResponse(responseCode = "401", description = "Unauthorized")
        })
        @GetMapping("/me")
        public Mono<ResponseEntity<UserResponse>> getAuthenticatedUser(@AuthenticationPrincipal UserDetails userDetails) {
                log.debug("Received request to get authenticated user: {}", userDetails.getUsername());

                // Usamos userDetails.getUsername() que contiene el email inyectado por el filtro
                return userService.getUserByEmail(userDetails.getUsername())
                        .map(ResponseEntity::ok)
                        .doOnError(error -> log.error("Error retrieving authenticated user: {}", error.getMessage()));
        }

}