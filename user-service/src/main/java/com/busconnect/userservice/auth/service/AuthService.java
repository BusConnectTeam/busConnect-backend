package com.busconnect.userservice.auth.service;

import com.busconnect.userservice.auth.dto.request.LoginRequest;
import com.busconnect.userservice.auth.dto.response.AuthResponse;
import com.busconnect.userservice.dto.request.CreateUserRequest;
import reactor.core.publisher.Mono;

/**
 * Service interface for authentication operations.
 */
public interface AuthService {

    /**
     * Registers a new user.
     *
     * @param request the registration request containing user details
     * @return Mono containing the authentication response with token
     */
    Mono<AuthResponse> register(CreateUserRequest request);

    /**
     * Authenticates a user.
     *
     * @param request the login request containing credentials
     * @return Mono containing the authentication response with token
     */
    Mono<AuthResponse> login(LoginRequest request);
}
