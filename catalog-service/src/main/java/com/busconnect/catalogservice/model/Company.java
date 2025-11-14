package com.busconnect.catalogservice.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "companies", schema = "catalog")
public class Company {

    @Id
    private UUID id;

    @Column("external_company_id")
    private String externalCompanyId; // Para futuro company-service

    @NotBlank(message = "{company.name.required}")
    @Column("name")
    private String name;

    @Column("contact_email")
    private String contactEmail;

    @Column("contact_phone")
    private String contactPhone;

    @Column("website")
    private String website;

    @NotNull
    @Column("verified")
    private Boolean verified = false;

    @DecimalMin(value = "0.0", message = "{company.rating.min}")
    @DecimalMax(value = "5.0", message = "{company.rating.max}")
    @Column("rating")
    private BigDecimal rating;

    @NotNull
    @Column("is_active")
    private Boolean active = true;

    @CreatedDate
    @Column("created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column("updated_at")
    private LocalDateTime updatedAt;

    // Constructor conveniente
    public Company(String name, String contactEmail, String contactPhone) {
        this.name = name;
        this.contactEmail = contactEmail;
        this.contactPhone = contactPhone;
        this.verified = false;
        this.active = true;
    }
}
