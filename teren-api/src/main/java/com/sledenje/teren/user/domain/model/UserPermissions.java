package com.sledenje.teren.user.domain.model;

/**
 * Pravice uporabnika v aplikaciji Teren. Preverja jih aplikacija, preden v imenu uporabnika pokliče API.
 */
public record UserPermissions(
        boolean canManageUsers,
        boolean canViewAllOrders,
        boolean canExportData,
        boolean canSendEmail,
        boolean canEditOrders,
        boolean canManageClients,
        boolean canManageVehicles,
        boolean canManagePlan) {

    public static final UserPermissions NONE = new UserPermissions(false, false, false, false, false, false, false, false);

}
