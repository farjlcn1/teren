package com.sledenje.teren.user.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * Vsa polja so obvezna, da izpuščeno polje pomotoma ne odvzame pravice.
 */
@Schema(description = "Aktivnost in vse pravice uporabnika hkrati")
public record UpdateUserAccessRequest(
        @NotNull Boolean isActive,
        @NotNull Boolean canManageUsers,
        @NotNull Boolean canViewAllOrders,
        @NotNull Boolean canExportData,
        @NotNull Boolean canSendEmail,
        @NotNull Boolean canEditOrders,
        @NotNull Boolean canManageClients,
        @NotNull Boolean canManageVehicles,
        @NotNull Boolean canManagePlan) {
}
