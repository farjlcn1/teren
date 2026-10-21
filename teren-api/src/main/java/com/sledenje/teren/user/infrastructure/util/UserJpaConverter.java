package com.sledenje.teren.user.infrastructure.util;

import com.sledenje.teren.user.domain.model.StoredCredentials;
import com.sledenje.teren.user.domain.model.User;
import com.sledenje.teren.user.domain.model.UserPermissions;
import com.sledenje.teren.user.infrastructure.persistence.UserJpaEntity;

/**
 * Pretvorbe med JPA entiteto in domenskim modelom uporabnika.
 */
public final class UserJpaConverter {

    private UserJpaConverter() {
    }

    public static User toDomain(UserJpaEntity entity) {
        return new User(
                entity.getId(),
                entity.getEmail(),
                entity.getFullName(),
                entity.isActive(),
                new UserPermissions(
                        entity.isCanManageUsers(),
                        entity.isCanViewAllOrders(),
                        entity.isCanExportData(),
                        entity.isCanSendEmail(),
                        entity.isCanEditOrders(),
                        entity.isCanManageClients(),
                        entity.isCanManageVehicles(),
                        entity.isCanManagePlan()),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    public static StoredCredentials toCredentials(UserJpaEntity entity) {
        return new StoredCredentials(toDomain(entity), entity.getPasswordHash());
    }

    public static void applyAccess(UserJpaEntity entity, boolean isActive, UserPermissions permissions) {
        entity.updateAccess(
                isActive,
                permissions.canManageUsers(),
                permissions.canViewAllOrders(),
                permissions.canExportData(),
                permissions.canSendEmail(),
                permissions.canEditOrders(),
                permissions.canManageClients(),
                permissions.canManageVehicles(),
                permissions.canManagePlan());
    }

}
