package com.busconnect.userservice.dto.request;

import com.busconnect.userservice.model.UserRole;
import jakarta.validation.constraints.Email;
import lombok.Data;

@Data
public class UpdateUserRequest {

    @Email(message = "{email.invalid}")
    private String email;

    private String firstName;

    private String lastName;

    private String phone;

    private UserRole role;
}
