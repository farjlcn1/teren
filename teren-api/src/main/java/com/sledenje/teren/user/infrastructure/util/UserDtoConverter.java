package com.sledenje.teren.user.infrastructure.util;

import com.sledenje.teren.user.domain.model.NewUser;
import com.sledenje.teren.user.domain.model.User;
import com.sledenje.teren.user.domain.model.UserPermissions;
import com.sledenje.teren.user.presentation.dto.CreateUserRequest;
import com.sledenje.teren.user.presentation.dto.UpdateUserAccessRequest;
import com.sledenje.teren.user.presentation.dto.UserDto;

/**
 * Pretvorbe med DTO-ji presentation nivoja in domenskim modelom uporabnika.
 */
public final class UserDtoConverter {

    private UserDtoConverter() {
    }

    public static UserDto toDto(User user) {
        UserPermissions permissions = user.permissions();
        return new UserDto(
                user.id(),
                user.email(),
                user.fullName(),
                user.isActive(),
                permissions.canManageUsers(),
                permissions.canViewAllOrders(),
                permissions.canExportData(),
                permissions.canSendEmail(),
                permissions.canEditOrders(),
                permissions.canManageClients(),
                permissions.canManageVehicles(),
                permissions.canManagePlan(),
                user.sessionsValidFrom(),
                user.createdAt(),
                user.updatedAt());
    }

    public static NewUser toNewUser(CreateUserRequest request) {
        return new NewUser(request.email(), request.fullName(), request.password());
    }

    public static UserPermissions toPermissions(UpdateUserAccessRequest request) {
        return new UserPermissions(
                request.canManageUsers(),
                request.canViewAllOrders(),
                request.canExportData(),
                request.canSendEmail(),
                request.canEditOrders(),
                request.canManageClients(),
                request.canManageVehicles(),
                request.canManagePlan());
    }

}
