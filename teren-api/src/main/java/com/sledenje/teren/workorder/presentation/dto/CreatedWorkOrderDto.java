package com.sledenje.teren.workorder.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Ustvarjen nalog")
public record CreatedWorkOrderDto(
        String id,
        @Schema(description = "Številka naloga DDMMLLLLNNN", example = "21102026001") String ident) {
}
