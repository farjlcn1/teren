package com.sledenje.teren.workorder.presentation.dto;

import com.sledenje.teren.workorder.domain.model.InstallerName;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Monter")
public record WorkOrderInstallerDto(
        String id,
        InstallerName name,
        @Schema(description = "Ime monterja pri OSTALO", nullable = true) String otherText) {
}
