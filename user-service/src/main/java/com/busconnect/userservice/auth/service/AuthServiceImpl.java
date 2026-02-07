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

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final UserService userService;

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
                            .userId(savedUser.getId())
                            .email(savedUser.getEmail())
                            .firstName(savedUser.getFirstName())
                            .lastName(savedUser.getLastName())
                            .role(savedUser.getRole().name())
                            .issuedAt(LocalDateTime.now())
                            .build();
                });
    }

    @Override
    public Mono<AuthResponse> login(LoginRequest request) {
        return userRepository.findByEmail(request.getEmail())
                .switchIfEmpty(Mono.error(new BadCredentialsException("Invalid credentials")))
                .flatMap(user -> {
                    if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                        return Mono.error(new BadCredentialsException("Invalid credentials"));
                    }
                    if (!user.isActive()) {
                        return Mono.error(new BadCredentialsException("User is not active"));
                    }

                    String token = jwtUtil.generateToken(mapToUserDetails(user));

                    return Mono.just(AuthResponse.builder()
                            .token(token)
                            .userId(user.getId())
                            .email(user.getEmail())
                            .firstName(user.getFirstName())
                            .lastName(user.getLastName())
                            .role(user.getRole().name())
                            .issuedAt(LocalDateTime.now())
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
