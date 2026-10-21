package com.sledenje.teren.user.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "Uporabnik s pravicami (brez gesla)")
public record UserDto(
        @Schema(description = "ID uporabnika", example = "cmd8x2k0q0000abcd1234efgh")
        String id,
        @Schema(description = "Email (male črke)", example = "janez.novak@example.com")
        String email,
        @Schema(description = "Ime in priimek", example = "Janez Novak")
        String fullName,
        @Schema(description = "Onemogočen uporabnik se ne more prijaviti")
        boolean isActive,
        boolean canManageUsers,
        boolean canViewAllOrders,
        boolean canExportData,
        boolean canSendEmail,
        boolean canEditOrders,
        boolean canManageClients,
        boolean canManageVehicles,
        boolean canManagePlan,
        @Schema(description = "Seje (piškotki aplikacije), izdane pred tem časom, ne veljajo (UTC)", nullable = true)
        Instant sessionsValidFrom,
        @Schema(description = "Čas vnosa (UTC)")
        Instant createdAt,
        @Schema(description = "Čas zadnje spremembe (UTC)")
        Instant updatedAt) {
}
