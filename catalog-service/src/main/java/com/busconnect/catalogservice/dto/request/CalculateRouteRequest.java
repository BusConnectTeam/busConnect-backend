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
    private String originMunicipality;

    @NotBlank(message = "{municipality.required}")
    private String destinationMunicipality;

    /**
     * Indica si se debe forzar una nueva consulta a OpenRouteService
     * ignorando el cache. Por defecto false.
     */
    private boolean forceRefresh = false;
}
