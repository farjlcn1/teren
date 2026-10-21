package com.sledenje.teren.workorder.presentation.dto;

import com.sledenje.teren.common.domain.model.InstallerName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Monter")
public record InstallerRequest(
        @NotNull(message = "Izberi monterja.") InstallerName name,
        @Schema(description = "Ime monterja; obvezno pri OSTALO", nullable = true) String otherText) {
}
