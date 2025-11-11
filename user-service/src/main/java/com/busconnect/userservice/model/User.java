package com.busconnect.userservice.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "users", schema = "user_service")
@Access(AccessType.FIELD)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Email(message = "{email.invalid}")
    @NotBlank(message = "{email.required}")
    @Column(nullable = false, unique = true, length = 254)
    private String email;

    @NotBlank(message = "{password.required}")
    @Column(nullable = false, length = 255)
    private String passwordHash;

    @NotBlank(message = "{firstName.required}")
    @Column(nullable = false, length = 255)
    private String firstName;

    @NotBlank(message = "{lastName.required}")
    @Column(nullable = false, length = 255)
    private String lastName;

    @Column(length = 20)
    private String phone;

    @NotNull(message = "{role.required}")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

}
