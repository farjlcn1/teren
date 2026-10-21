package com.sledenje.teren.user.infrastructure.persistence;

import com.sledenje.teren.common.infrastructure.persistence.TimestampedJpaEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class UserJpaEntity extends TimestampedJpaEntity {

    @Id
    private String id;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false)
    private boolean isActive;

    @Column(nullable = false)
    private boolean canManageUsers;

    @Column(nullable = false)
    private boolean canViewAllOrders;

    @Column(nullable = false)
    private boolean canExportData;

    @Column(nullable = false)
    private boolean canSendEmail;

    @Column(nullable = false)
    private boolean canEditOrders;

    @Column(nullable = false)
    private boolean canManageClients;

    @Column(nullable = false)
    private boolean canManageVehicles;

    @Column(nullable = false)
    private boolean canManagePlan;

    protected UserJpaEntity() {
    }

    /**
     * Nov uporabnik je aktiven in nima pravic (tako kot privzete vrednosti v Prisma shemi).
     */
    public UserJpaEntity(String id, String email, String fullName, String passwordHash) {
        this.id = id;
        this.email = email;
        this.fullName = fullName;
        this.passwordHash = passwordHash;
        this.isActive = true;
    }

    public void updateAccess(boolean isActive, boolean canManageUsers, boolean canViewAllOrders, boolean canExportData,
            boolean canSendEmail, boolean canEditOrders, boolean canManageClients, boolean canManageVehicles,
            boolean canManagePlan) {
        this.isActive = isActive;
        this.canManageUsers = canManageUsers;
        this.canViewAllOrders = canViewAllOrders;
        this.canExportData = canExportData;
        this.canSendEmail = canSendEmail;
        this.canEditOrders = canEditOrders;
        this.canManageClients = canManageClients;
        this.canManageVehicles = canManageVehicles;
        this.canManagePlan = canManagePlan;
    }

    public void changePasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    @Override
    public String getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getFullName() {
        return fullName;
    }

    public boolean isActive() {
        return isActive;
    }

    public boolean isCanManageUsers() {
        return canManageUsers;
    }

    public boolean isCanViewAllOrders() {
        return canViewAllOrders;
    }

    public boolean isCanExportData() {
        return canExportData;
    }

    public boolean isCanSendEmail() {
        return canSendEmail;
    }

    public boolean isCanEditOrders() {
        return canEditOrders;
    }

    public boolean isCanManageClients() {
        return canManageClients;
    }

    public boolean isCanManageVehicles() {
        return canManageVehicles;
    }

    public boolean isCanManagePlan() {
        return canManagePlan;
    }

}
