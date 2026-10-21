package com.sledenje.teren.vehicle.infrastructure.util;

import com.sledenje.teren.vehicle.domain.model.Vehicle;
import com.sledenje.teren.vehicle.domain.model.VehicleSyncConfig;
import com.sledenje.teren.vehicle.infrastructure.persistence.VehicleJpaEntity;
import com.sledenje.teren.vehicle.infrastructure.persistence.VehicleSyncConfigJpaEntity;

/**
 * Pretvorbe med JPA entitetami in domenskim modelom vozila.
 */
public final class VehicleJpaConverter {

    private VehicleJpaConverter() {
    }

    public static Vehicle toDomain(VehicleJpaEntity entity) {
        return new Vehicle(
                entity.getId(),
                entity.getPlate(),
                entity.getSource(),
                entity.getExternalId(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    public static VehicleSyncConfig toDomain(VehicleSyncConfigJpaEntity entity) {
        return new VehicleSyncConfig(
                entity.getApiUrl(),
                entity.getApiToken(),
                entity.getLastSyncAt(),
                entity.getLastSyncStatus());
    }

}
