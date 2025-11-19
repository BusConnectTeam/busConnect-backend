package com.busconnect.catalogservice.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "municipalities", schema = "catalog")
public class Municipality {

    @Id
    private UUID id;

    @NotBlank(message = "{municipality.name.required}")
    @Column("name")
    private String name;

    @NotBlank(message = "{municipality.normalized.required}")
    @Column("normalized_name")
    private String normalizedName;

    @NotBlank(message = "{municipality.province.required}")
    @Column("province")
    private String province;

    @Column("latitude")
    private BigDecimal latitude;

    @Column("longitude")
    private BigDecimal longitude;

    @Column("postal_codes")
    private String postalCodes;

    @NotNull
    @Column("is_active")
    private Boolean active = true;

    @CreatedDate
    @Column("created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column("updated_at")
    private LocalDateTime updatedAt;

    // Constructor conveniente para crear municipio básico
    public Municipality(String name, String province, BigDecimal latitude, BigDecimal longitude) {
        this.name = name;
        this.normalizedName = name.toLowerCase().trim();
        this.province = province;
        this.latitude = latitude;
        this.longitude = longitude;
        this.active = true;
    }
}