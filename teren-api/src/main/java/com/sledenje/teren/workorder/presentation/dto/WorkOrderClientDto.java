package com.sledenje.teren.workorder.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Stranka naloga")
public record WorkOrderClientDto(
        String id,
        String name,
        @Schema(nullable = true) String address,
        @Schema(nullable = true) String contactInfo) {
}
