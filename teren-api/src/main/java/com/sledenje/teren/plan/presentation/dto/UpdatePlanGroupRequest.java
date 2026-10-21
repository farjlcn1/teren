package com.sledenje.teren.plan.presentation.dto;

import com.sledenje.teren.common.domain.model.InstallerName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

@Schema(description = "Novi podatki dogodka. Prekrivajoči se dogodki istega monterja se zamaknejo za njegov konec.")
public record UpdatePlanGroupRequest(
        @NotBlank(message = "Izberi stranko.") String clientId,
        @NotNull(message = "Vnesi začetek.") Instant startAt,
        @Schema(description = "Obvezno pri urejanju", nullable = true) Instant endAt,
        @Schema(nullable = true) String note,
        @Schema(nullable = true) String contact,
        @Schema(nullable = true) InstallerName expectedInstaller,
        @Schema(description = "Obvezno pri OSTALO", nullable = true) String expectedInstallerOtherText) {
}
