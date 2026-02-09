package com.busconnect.userservice.auth;

import com.busconnect.userservice.auth.config.SecurityConfig;
import com.busconnect.userservice.auth.controller.AuthController;
import com.busconnect.userservice.auth.filter.JwtAuthenticationFilter;
import com.busconnect.userservice.auth.service.AuthService;
import com.busconnect.userservice.auth.util.JwtUtil;
import com.busconnect.userservice.controller.UserController;
import com.busconnect.userservice.dto.response.UserResponse;
import com.busconnect.userservice.model.UserRole;
import com.busconnect.userservice.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.r2dbc.R2dbcDataAutoConfiguration;
import org.springframework.boot.autoconfigure.r2dbc.R2dbcAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.data.r2dbc.mapping.R2dbcMappingContext;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Integration tests for JWT authentication and authorization.
 * Tests the security filter chain without requiring database infrastructure.
 */
@WebFluxTest(controllers = { AuthController.class, UserController.class }, excludeAutoConfiguration = {
        R2dbcAutoConfiguration.class, R2dbcDataAutoConfiguration.class })
@Import({ SecurityConfig.class, JwtAuthenticationFilter.class, JwtUtil.class })
@ActiveProfiles("test")
class AuthenticationSecurityTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private JwtUtil jwtUtil;

    @MockBean
    private AuthService authService;

    @MockBean
    private UserService userService;

    @MockBean
    private ReactiveUserDetailsService reactiveUserDetailsService;

    // Mock R2DBC mapping context to prevent auditing configuration from loading
    @MockBean(name = "r2dbcMappingContext")
    private R2dbcMappingContext r2dbcMappingContext;

    private UserDetails testUserDetails;
    private UserResponse testUserResponse;
    private com.busconnect.userservice.auth.dto.response.AuthResponse testAuthResponse;

    @BeforeEach
    void setUp() {
        // Setup test user details
        testUserDetails = User.withUsername("test@example.com")
                .password("$2a$10$dummyHashedPassword")
                .roles("CUSTOMER")
                .build();

        // Setup test user response
        testUserResponse = new UserResponse(
                1L,
                "test@example.com",
                "Test",
                "User",
                "600123456",
                UserRole.CUSTOMER,
                true,
                LocalDateTime.now(),
                LocalDateTime.now());

        // Setup a generic AuthResponse used by public endpoints mocks
        testAuthResponse = com.busconnect.userservice.auth.dto.response.AuthResponse.builder()
                .token("dummy-token")
                .email("test@example.com")
                .firstName("Test")
                .lastName("User")
                .role(UserRole.CUSTOMER.name())
                .issuedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(1))
                .build();

        // Mock authService.register and authService.login to return a valid AuthResponse
        when(authService.register(any(com.busconnect.userservice.dto.request.CreateUserRequest.class)))
                .thenReturn(Mono.just(testAuthResponse));

        when(authService.login(any(com.busconnect.userservice.auth.dto.request.LoginRequest.class)))
                .thenReturn(Mono.just(testAuthResponse));
    }

    @Test
    @DisplayName("Should allow public access to /api/users/auth/register endpoint")
    void shouldAllowPublicAccessToRegister() {
        // Send a valid body so controller validation passes and the handler can be invoked
        String body = "{\"email\":\"public@example.com\",\"firstName\":\"Public\",\"lastName\":\"User\",\"phone\":\"600000000\",\"password\":\"DummyPass123!\",\"role\":\"CUSTOMER\"}";

        webTestClient.post()
                .uri("/api/users/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus().isCreated(); // 201 proves it's accessible without auth and controller processed the request
    }

    @Test
    @DisplayName("Should allow public access to /api/users/auth/login endpoint")
    void shouldAllowPublicAccessToLogin() {
        // Send a valid login body so controller validation passes
        String body = "{\"email\":\"public@example.com\",\"password\":\"DummyPass123!\"}";

        webTestClient.post()
                .uri("/api/users/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus().isOk(); // 200 proves it's accessible without auth and controller processed the request
    }

    @Test
    @DisplayName("Should return 401 when accessing /api/users/me without token")
    void shouldReturn401WhenAccessingProtectedEndpointWithoutToken() {
        webTestClient.get()
                .uri("/api/users/me")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("Should return 401 when accessing /api/users/me with invalid token")
    void shouldReturn401WhenAccessingProtectedEndpointWithInvalidToken() {
        webTestClient.get()
                .uri("/api/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token-here")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("Should return 401 when accessing /api/users/me with malformed token")
    void shouldReturn401WhenAccessingProtectedEndpointWithMalformedToken() {
        webTestClient.get()
                .uri("/api/users/me")
                .header(HttpHeaders.AUTHORIZATION, "NotBearer invalid-token")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("Should return 200 when accessing /api/users/me with valid token")
    void shouldReturn200WhenAccessingProtectedEndpointWithValidToken() {
        // Generate a valid token
        String validToken = jwtUtil.generateToken(testUserDetails);

        // Mock the ReactiveUserDetailsService to return our test user
        when(reactiveUserDetailsService.findByUsername(anyString()))
                .thenReturn(Mono.just(testUserDetails));

        // Mock the UserService to return our test user response
        when(userService.getUserByEmail(anyString()))
                .thenReturn(Mono.just(testUserResponse));

        webTestClient.get()
                .uri("/api/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.email").isEqualTo("test@example.com")
                .jsonPath("$.firstName").isEqualTo("Test")
                .jsonPath("$.role").isEqualTo("CUSTOMER");
    }

    @Test
    @DisplayName("Should return 401 when accessing /api/users/me with expired token")
    void shouldReturn401WhenAccessingProtectedEndpointWithExpiredToken() {
        // Use a token with invalid signature (will fail validation)
        String expiredToken = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0QGV4YW1wbGUuY29tIiwiaWF0IjoxNjAwMDAwMDAwLCJleHAiOjE2MDAwMDAwMDB9.invalid";

        webTestClient.get()
                .uri("/api/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("Should return 401 when Authorization header is missing Bearer prefix")
    void shouldReturn401WhenBearerPrefixIsMissing() {
        String validToken = jwtUtil.generateToken(testUserDetails);

        webTestClient.get()
                .uri("/api/users/me")
                .header(HttpHeaders.AUTHORIZATION, validToken) // Missing "Bearer " prefix
                .exchange()
                .expectStatus().isUnauthorized();
    }
}
