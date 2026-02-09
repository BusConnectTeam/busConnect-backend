package com.busconnect.userservice.auth.service;

import com.busconnect.userservice.auth.dto.request.LoginRequest;
import com.busconnect.userservice.auth.dto.response.AuthResponse;
import com.busconnect.userservice.auth.util.JwtUtil;
import com.busconnect.userservice.dto.request.CreateUserRequest;
import com.busconnect.userservice.model.User;
import com.busconnect.userservice.model.UserRole;
import com.busconnect.userservice.repository.UserRepository;
import com.busconnect.userservice.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

import static org.springframework.security.core.userdetails.User.withUsername;

/**
 * Implementation of AuthService using Spring WebFlux and Reactive patterns.
 * This service handles the business logic for user self-registration and
 * secure authentication using JWT.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final UserService userService;

    /**
     * Registers a new user in the system.
     * Hardcodes the role to CUSTOMER to prevent privilege escalation from public endpoints.
     * * @param request The user creation data.
     * @return A Mono containing the AuthResponse with the first JWT.
     */
    @Override
    public Mono<AuthResponse> register(CreateUserRequest request) {
        log.info("Registering new user with email: {}", request.getEmail());
        // Force role to CUSTOMER for public registration
        request.setRole(UserRole.CUSTOMER);

        return userService.createUser(request)
                .map(savedUser -> {
                    String token = jwtUtil.generateToken(withUsername(savedUser.getEmail())
                            .password("") // Not needed for token generation
                            .roles(savedUser.getRole().name())
                            .build());

                    return AuthResponse.builder()
                            .token(token)
                            .email(savedUser.getEmail())
                            .firstName(savedUser.getFirstName())
                            .lastName(savedUser.getLastName())
                            .role(savedUser.getRole().name())
                            .issuedAt(LocalDateTime.now())
                            .expiresAt(LocalDateTime.now().plus(jwtUtil.getExpirationTime(),
                                    java.time.temporal.ChronoUnit.MILLIS))
                            .build();
                });
    }

    /**
     * Authenticates a user and generates a session token.
     * Validates credentials, user existence, and active status.
     * * @param request The login credentials (email and password).
     * @return A Mono with the AuthResponse and the session JWT.
     * @throws BadCredentialsException if authentication fails.
     */
    @Override
    public Mono<AuthResponse> login(LoginRequest request) {
        return userRepository.findByEmail(request.getEmail())
                .switchIfEmpty(Mono.error(new BadCredentialsException("auth.login.invalid")))
                .flatMap(user -> {
                    if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                        return Mono.error(new BadCredentialsException("auth.login.invalid"));
                    }
                    if (!user.isActive()) {
                        return Mono.error(new BadCredentialsException("auth.user.inactive"));
                    }

                    String token = jwtUtil.generateToken(mapToUserDetails(user));

                    return Mono.just(AuthResponse.builder()
                            .token(token)
                            .email(user.getEmail())
                            .firstName(user.getFirstName())
                            .lastName(user.getLastName())
                            .role(user.getRole().name())
                            .issuedAt(LocalDateTime.now())
                            .expiresAt(LocalDateTime.now().plus(jwtUtil.getExpirationTime(),
                                    java.time.temporal.ChronoUnit.MILLIS))
                            .build());
                });
    }

    private UserDetails mapToUserDetails(User user) {
        return withUsername(user.getEmail())
                .password(user.getPassword())
                .roles(user.getRole().name())
                .build();
    }
}
