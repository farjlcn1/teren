package com.sledenje.teren.workorder.presentation.dto;

import com.sledenje.teren.workorder.domain.model.OptionType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Priključena opcija")
public record WorkOrderOptionDto(String id, OptionType optionType, @Schema(nullable = true) String comment) {
}
