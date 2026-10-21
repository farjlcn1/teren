package com.sledenje.teren.workorder.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "Urejanje naloga")
public record WorkOrderEditDto(
        String id,
        @Schema(description = "Čas urejanja (UTC)") Instant editedAt,
        UserReferenceDto editedBy,
        @Schema(description = "Spremembe kot JSON besedilo") String changesJson) {
}
