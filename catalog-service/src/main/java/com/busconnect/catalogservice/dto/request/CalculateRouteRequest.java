package com.busconnect.catalogservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CalculateRouteRequest {

    @NotBlank(message = "{municipality.required}")
    private String origin;

    @NotBlank(message = "{municipality.required}")
    private String destination;
}
