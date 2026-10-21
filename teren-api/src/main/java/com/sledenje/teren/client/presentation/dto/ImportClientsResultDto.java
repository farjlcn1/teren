package com.sledenje.teren.client.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Izid uvoza strank")
public record ImportClientsResultDto(
        @Schema(description = "Število dodanih strank", example = "12")
        int createdCount) {
}
