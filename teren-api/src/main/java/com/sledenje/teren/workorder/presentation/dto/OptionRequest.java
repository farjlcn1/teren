package com.sledenje.teren.workorder.presentation.dto;

import com.sledenje.teren.workorder.domain.model.OptionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Priključena opcija")
public record OptionRequest(
        @NotNull(message = "Izberi opcijo.") OptionType optionType,
        @Schema(nullable = true) String comment) {
}
