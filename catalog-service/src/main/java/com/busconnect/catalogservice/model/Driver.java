package com.busconnect.catalogservice.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.UUID;

/**
 * Entidad que representa un conductor de autobús.
 * Incluye información personal, licencia y experiencia.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "drivers", schema = "catalog")
public class Driver {

    @Id
    private UUID id;

    @NotNull(message = "El ID de la empresa es obligatorio")
    @Column("company_id")
    private UUID companyId;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 50, message = "El nombre no puede exceder 50 caracteres")
    @Column("first_name")
    private String firstName;

    @NotBlank(message = "Los apellidos son obligatorios")
    @Size(max = 100, message = "Los apellidos no pueden exceder 100 caracteres")
    @Column("last_name")
    private String lastName;

    @NotBlank(message = "El DNI es obligatorio")
    @Size(max = 15, message = "El DNI no puede exceder 15 caracteres")
    @Column("dni")
    private String dni;

    @Email(message = "El email debe ser válido")
    @Size(max = 100, message = "El email no puede exceder 100 caracteres")
    @Column("email")
    private String email;

    @NotBlank(message = "El teléfono es obligatorio")
    @Size(max = 20, message = "El teléfono no puede exceder 20 caracteres")
    @Column("phone")
    private String phone;

    @NotNull(message = "La fecha de nacimiento es obligatoria")
    @Column("birth_date")
    private LocalDate birthDate;

    @NotNull(message = "La fecha de contratación es obligatoria")
    @Column("hire_date")
    private LocalDate hireDate;

    @NotBlank(message = "El número de licencia es obligatorio")
    @Size(max = 30, message = "El número de licencia no puede exceder 30 caracteres")
    @Column("license_number")
    private String licenseNumber;

    @NotNull(message = "La fecha de expiración de la licencia es obligatoria")
    @Column("license_expiry_date")
    private LocalDate licenseExpiryDate;

    @NotBlank(message = "El tipo de licencia es obligatorio")
    @Size(max = 10, message = "El tipo de licencia no puede exceder 10 caracteres")
    @Column("license_type")
    private String licenseType; // D, D+E

    @Min(value = 0, message = "Los años de experiencia no pueden ser negativos")
    @Column("years_experience")
    private Integer yearsExperience = 0;

    @Size(max = 100, message = "Los idiomas no pueden exceder 100 caracteres")
    @Column("languages")
    private String languages; // es,ca,en,fr

    @Size(max = 255, message = "La URL de la foto no puede exceder 255 caracteres")
    @Column("photo_url")
    private String photoUrl;

    @NotNull
    @Column("is_active")
    private Boolean active = true;

    @CreatedDate
    @Column("created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column("updated_at")
    private LocalDateTime updatedAt;

    /**
     * Obtiene el nombre completo del conductor.
     */
    public String getFullName() {
        return firstName + " " + lastName;
    }

    /**
     * Calcula la edad actual del conductor.
     */
    public int getAge() {
        if (birthDate == null) {
            return 0;
        }
        return Period.between(birthDate, LocalDate.now()).getYears();
    }

    /**
     * Verifica si la licencia del conductor está vigente.
     */
    public boolean hasValidLicense() {
        if (licenseExpiryDate == null) {
            return false;
        }
        return licenseExpiryDate.isAfter(LocalDate.now());
    }

    /**
     * Verifica si la licencia expira pronto (en los próximos 90 días).
     */
    public boolean isLicenseExpiringSoon() {
        if (licenseExpiryDate == null) {
            return true;
        }
        return licenseExpiryDate.isBefore(LocalDate.now().plusDays(90));
    }

    /**
     * Obtiene los idiomas como array.
     */
    public String[] getLanguagesArray() {
        if (languages == null || languages.isEmpty()) {
            return new String[0];
        }
        return languages.split(",");
    }

    /**
     * Constructor de conveniencia para crear un conductor básico.
     */
    public Driver(UUID companyId, String firstName, String lastName, String dni, String phone,
                  LocalDate birthDate, LocalDate hireDate, String licenseNumber,
                  LocalDate licenseExpiryDate, String licenseType) {
        this.companyId = companyId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.dni = dni;
        this.phone = phone;
        this.birthDate = birthDate;
        this.hireDate = hireDate;
        this.licenseNumber = licenseNumber;
        this.licenseExpiryDate = licenseExpiryDate;
        this.licenseType = licenseType;
        this.active = true;
    }
}
