package com.busconnect.catalogservice.model;

import jakarta.validation.constraints.Email;
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

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad que representa una empresa de autobuses.
 * Contiene información de la empresa, contacto y estado.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "bus_companies", schema = "catalog")
public class BusCompany {

    @Id
    private UUID id;

    @NotBlank(message = "El nombre de la empresa es obligatorio")
    @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
    @Column("name")
    private String name;

    @NotBlank(message = "La razón social es obligatoria")
    @Size(max = 150, message = "La razón social no puede exceder 150 caracteres")
    @Column("legal_name")
    private String legalName;

    @NotBlank(message = "El CIF es obligatorio")
    @Size(max = 15, message = "El CIF no puede exceder 15 caracteres")
    @Column("cif")
    private String cif;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email debe ser válido")
    @Column("email")
    private String email;

    @NotBlank(message = "El teléfono es obligatorio")
    @Size(max = 20, message = "El teléfono no puede exceder 20 caracteres")
    @Column("phone")
    private String phone;

    @Size(max = 200, message = "La dirección no puede exceder 200 caracteres")
    @Column("address")
    private String address;

    @Size(max = 100, message = "La ciudad no puede exceder 100 caracteres")
    @Column("city")
    private String city;

    @Size(max = 10, message = "El código postal no puede exceder 10 caracteres")
    @Column("postal_code")
    private String postalCode;

    @Size(max = 100, message = "El sitio web no puede exceder 100 caracteres")
    @Column("website")
    private String website;

    @Size(max = 255, message = "La URL del logo no puede exceder 255 caracteres")
    @Column("logo_url")
    private String logoUrl;

    @Column("founded_year")
    private Integer foundedYear;

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
     * Constructor de conveniencia para crear una empresa con datos básicos.
     */
    public BusCompany(String name, String legalName, String cif, String email, String phone, String city) {
        this.name = name;
        this.legalName = legalName;
        this.cif = cif;
        this.email = email;
        this.phone = phone;
        this.city = city;
        this.active = true;
    }
}
