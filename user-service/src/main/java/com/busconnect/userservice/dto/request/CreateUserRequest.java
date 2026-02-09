package com.busconnect.userservice.dto.request;

import com.busconnect.userservice.model.UserRole;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CreateUserRequest {

    @Email(message = "{email.invalid}")
    @NotBlank(message = "{email.required}")
    private String email;

    @NotBlank(message = "{firstName.required}")
    private String firstName;

    @NotBlank(message = "{lastName.required}")
    private String lastName;

    private String phone;

    @NotBlank(message = "{password.required}")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$", message = "{password.weak}")
    private String password;

    @NotNull(message = "{role.required}")
    private UserRole role;

}
