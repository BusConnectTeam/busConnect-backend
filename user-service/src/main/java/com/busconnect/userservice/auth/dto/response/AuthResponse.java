package com.busconnect.userservice.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for authentication response.
 * Contains JWT token and user information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String token;
    private String tokenType = "Bearer";

    private Long userId;
    private String email;
    private String firstName;
    private String lastName;
    private String role;

    private LocalDateTime issuedAt;
    private LocalDateTime expiresAt;
}
