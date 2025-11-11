package com.busconnect.userservice.dto.request;

import com.busconnect.userservice.model.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateUserRequest {

    @Email(message = "{email.invalid}")
    @NotBlank(message = "{email.required}")
    private String email;

    @NotBlank(message = "{password.required}")
    private String password;

    @NotBlank(message = "{firstName.required}")
    private String firstName;

    @NotBlank(message = "{lastName.required}")
    private String lastName;

    private String phone;

    @NotNull(message = "{role.required}")
    private UserRole role;

}
