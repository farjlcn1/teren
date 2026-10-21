package com.sledenje.teren.workorder.presentation.dto;

import com.sledenje.teren.workorder.domain.model.DeviceModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Model naprave")
public record DeviceModelRequest(
        @NotNull(message = "Izberi model naprave.") DeviceModel deviceModel,
        @Schema(description = "Ime naprave; obvezno pri OSTALO", nullable = true) String comment) {
}
