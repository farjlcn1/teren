package com.sledenje.teren.client.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Izid sinhronizacije strank")
public record ClientSyncResultDto(
        @Schema(description = "Število sinhroniziranih strank", example = "12")
        int syncedCount) {
}
