package com.sledenje.teren.vehicle.domain.repository;

import com.sledenje.teren.vehicle.domain.model.VehicleSyncConfig;
import java.time.Instant;
import java.util.Optional;

public interface VehicleSyncConfigRepository {

    Optional<VehicleSyncConfig> findSyncConfig();

    VehicleSyncConfig saveConnection(String apiUrl, String apiToken);

    void saveSyncResult(Instant syncedAt, String status);

}
