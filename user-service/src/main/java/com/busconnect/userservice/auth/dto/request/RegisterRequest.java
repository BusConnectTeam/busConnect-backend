package com.busconnect.userservice.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for registration request.
 * Contains user details for creating a new account.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @Email(message = "{email.invalid}")
    @NotBlank(message = "{email.required}")
    private String email;

    @NotBlank(message = "{password.required}")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$", message = "{password.weak}")
    private String password;

    @NotBlank(message = "{firstName.required}")
    private String firstName;

    @NotBlank(message = "{lastName.required}")
    private String lastName;

    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "{phone.invalid}")
    private String phone;
}
