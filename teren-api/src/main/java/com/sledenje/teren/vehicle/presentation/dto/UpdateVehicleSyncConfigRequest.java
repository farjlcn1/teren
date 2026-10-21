package com.sledenje.teren.vehicle.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Povezava do zunanjega sistema za sinhronizacijo vozil")
public record UpdateVehicleSyncConfigRequest(
        @Schema(description = "URL, ki z GET vrne JSON seznam vozil; prazen izbriše povezavo in žeton",
                example = "https://fleet.example.com/api/vozila", nullable = true)
        @Pattern(regexp = "^$|^https?://\\S+$", message = "Vnesi veljaven URL")
        String apiUrl,
        @Schema(description = "Bearer žeton; prazen obdrži obstoječega, če se URL ne spremeni", nullable = true)
        String apiToken) {
}
