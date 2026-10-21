package com.sledenje.teren.plan.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

@Schema(description = "Novo časovno okno dogodka (premik po koledarju)")
public record MovePlanGroupRequest(
        @NotNull(message = "Neveljaven datum/čas.") Instant startAt,
        @NotNull(message = "Neveljaven datum/čas.") Instant endAt) {
}
