package com.busconnect.userservice.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "users", schema = "user_service")
public class User {

    @Id
    private Long id;

    @Email(message = "{email.invalid}")
    @NotBlank(message = "{email.required}")
    @Column("email")
    private String email;

    @NotBlank(message = "{firstName.required}")
    @Column("first_name")
    private String firstName;

    @NotBlank(message = "{lastName.required}")
    @Column("last_name")
    private String lastName;

    @Column("phone")
    private String phone;

    @NotNull(message = "{role.required}")
    @Column("role")
    private UserRole role;

    @CreatedDate
    @Column("created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column("updated_at")
    private LocalDateTime updatedAt;

    @Column("is_active")
    private boolean isActive = true;

}
