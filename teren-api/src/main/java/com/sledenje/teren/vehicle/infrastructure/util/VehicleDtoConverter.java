package com.sledenje.teren.vehicle.infrastructure.util;

import com.sledenje.teren.vehicle.domain.model.Vehicle;
import com.sledenje.teren.vehicle.domain.model.VehicleImportResult;
import com.sledenje.teren.vehicle.domain.model.VehicleSyncConfig;
import com.sledenje.teren.vehicle.presentation.dto.ImportVehiclesResultDto;
import com.sledenje.teren.vehicle.presentation.dto.VehicleDto;
import com.sledenje.teren.vehicle.presentation.dto.VehicleSyncConfigDto;

/**
 * Pretvorbe med DTO-ji presentation nivoja in domenskim modelom vozila.
 */
public final class VehicleDtoConverter {

    private VehicleDtoConverter() {
    }

    public static VehicleDto toDto(Vehicle vehicle) {
        return new VehicleDto(
                vehicle.id(),
                vehicle.plate(),
                vehicle.source(),
                vehicle.externalId(),
                vehicle.createdAt(),
                vehicle.updatedAt());
    }

    public static ImportVehiclesResultDto toDto(VehicleImportResult importResult) {
        return new ImportVehiclesResultDto(importResult.createdCount(), importResult.skippedCount());
    }

    public static VehicleSyncConfigDto toDto(VehicleSyncConfig syncConfig) {
        return new VehicleSyncConfigDto(
                syncConfig.apiUrl(),
                syncConfig.hasToken(),
                syncConfig.lastSyncAt(),
                syncConfig.lastSyncStatus());
    }

}
