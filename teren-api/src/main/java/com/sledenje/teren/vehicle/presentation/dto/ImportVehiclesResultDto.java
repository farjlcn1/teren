package com.sledenje.teren.vehicle.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Izid uvoza vozil")
public record ImportVehiclesResultDto(
        @Schema(description = "Število dodanih vozil", example = "12")
        int createdCount,
        @Schema(description = "Število preskočenih (že obstajajo ali so se v uvozu ponovile)", example = "3")
        int skippedCount) {
}
