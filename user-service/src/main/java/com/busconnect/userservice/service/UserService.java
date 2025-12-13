package com.busconnect.userservice.service;

import com.busconnect.userservice.dto.request.CreateUserRequest;
import com.busconnect.userservice.dto.request.UpdateUserRequest;
import com.busconnect.userservice.dto.response.UserResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Servicio reactivo para la gestión de usuarios.
 */
public interface UserService {

    Mono<UserResponse> createUser(CreateUserRequest request);

    Mono<UserResponse> getUserById(Long id);

    Mono<UserResponse> getUserByEmail(String email);

    Mono<UserResponse> updateUser(Long id, UpdateUserRequest request);

    Flux<UserResponse> getAllUsers();

    Mono<Void> softDeleteUser(Long id);

    Mono<UserResponse> restoreUser(Long id);

    Mono<Void> deleteUserPermanently(Long id);


}
