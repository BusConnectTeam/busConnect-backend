package com.busconnect.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table (name = "auth_user")
@Access(AccessType.FIELD)
@Getter
@Setter
@ToString(exclude = {"passwordHash"}) // Seguridad
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //CREDENCIALS
    @Email(message = "{email.invalid}")
    @NotBlank(message = "{email.required}")
    @Column(nullable = false, unique = true, length = 254)
    private String email;

    @NotBlank(message = "{password.required}")
    @Column(nullable = false, length = 255)
    private String passwordHash;

    //SECURITY STATE
    @Column(name = "is_active", nullable = false)
    private Boolean active = true;

    @Column(name = "is_locked", nullable = false)
    private Boolean locked = false;

    @Column(name = "failed_login_attempts", nullable = false)
    @Min(0)
    private Integer failedLoginAttempts = 0;

    //TIMESTAMP AND AUDIT
    @Column(name = "last_login", updatable = true)
    private LocalDateTime lastLogin;

    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    //LISTENERS JPA
    @PrePersist
    protected void onPrePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.failedLoginAttempts == null) {
            this.failedLoginAttempts = 0;
        }
        if (this.active == null) {
            this.active = true;
        }
        if (this.locked == null) {
            this.locked = false;
        }
        if (this.updatedAt == null) {
            this.updatedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onPreUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
