package com.sledenje.teren.plan.presentation.dto;

import com.sledenje.teren.common.domain.model.InstallerName;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

@Schema(description = "Dogodek v planu s planiranimi nalogi (po času vnosa)")
public record PlanGroupDto(
        String id,
        String clientId,
        String clientName,
        @Schema(description = "Začetek (UTC)") Instant startAt,
        @Schema(description = "Konec (UTC)") Instant endAt,
        @Schema(nullable = true) String note,
        @Schema(nullable = true) String contact,
        @Schema(nullable = true) InstallerName expectedInstaller,
        @Schema(description = "Ime monterja pri OSTALO", nullable = true) String expectedInstallerOtherText,
        String createdById,
        List<PlannedTaskDto> tasks) {
}
