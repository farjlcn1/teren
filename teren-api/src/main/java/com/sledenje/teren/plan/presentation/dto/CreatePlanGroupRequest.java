package com.sledenje.teren.plan.presentation.dto;

import com.sledenje.teren.common.domain.model.InstallerName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;

@Schema(description = "Nov dogodek. Prekrivajoči se dogodki istega monterja se zamaknejo za njegov konec.")
public record CreatePlanGroupRequest(
        @NotBlank(message = "Izberi stranko.") String clientId,
        @NotNull(message = "Vnesi začetek.") Instant startAt,
        @Schema(description = "Neobvezno; privzeto ura po začetku", nullable = true) Instant endAt,
        @Schema(nullable = true) String note,
        @Schema(nullable = true) String contact,
        @Schema(nullable = true) InstallerName expectedInstaller,
        @Schema(description = "Obvezno pri OSTALO", nullable = true) String expectedInstallerOtherText,
        @Schema(description = "Uporabnik, ki dogodek ustvari") @NotBlank String createdById,
        @NotNull List<@Valid PlannedTaskRequest> tasks) {
}
