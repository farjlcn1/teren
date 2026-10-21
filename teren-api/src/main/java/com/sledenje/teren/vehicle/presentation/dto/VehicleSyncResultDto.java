package com.sledenje.teren.vehicle.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Izid sinhronizacije vozil")
public record VehicleSyncResultDto(
        @Schema(description = "Število sinhroniziranih vozil", example = "12")
        int syncedCount) {
}
