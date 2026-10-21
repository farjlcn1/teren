package com.sledenje.teren.vehicle.infrastructure.api.repository;

import com.sledenje.teren.vehicle.domain.model.VehicleSyncConfig;
import com.sledenje.teren.vehicle.domain.repository.VehicleSyncConfigRepository;
import com.sledenje.teren.vehicle.infrastructure.persistence.VehicleSyncConfigJpaEntity;
import com.sledenje.teren.vehicle.infrastructure.persistence.VehicleSyncConfigJpaRepository;
import com.sledenje.teren.vehicle.infrastructure.util.VehicleJpaConverter;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class VehicleSyncConfigRepositoryImpl implements VehicleSyncConfigRepository {

    private final VehicleSyncConfigJpaRepository vehicleSyncConfigJpaRepository;

    public VehicleSyncConfigRepositoryImpl(VehicleSyncConfigJpaRepository vehicleSyncConfigJpaRepository) {
        this.vehicleSyncConfigJpaRepository = vehicleSyncConfigJpaRepository;
    }

    @Override
    public Optional<VehicleSyncConfig> findSyncConfig() {
        return vehicleSyncConfigJpaRepository.findById(VehicleSyncConfigJpaEntity.DEFAULT_ID)
                .map(VehicleJpaConverter::toDomain);
    }

    @Override
    @Transactional
    public VehicleSyncConfig saveConnection(String apiUrl, String apiToken) {
        VehicleSyncConfigJpaEntity entity = findOrCreateDefault();
        entity.updateConnection(apiUrl, apiToken);
        return VehicleJpaConverter.toDomain(vehicleSyncConfigJpaRepository.save(entity));
    }

    @Override
    @Transactional
    public void saveSyncResult(Instant syncedAt, String status) {
        VehicleSyncConfigJpaEntity entity = findOrCreateDefault();
        entity.recordSyncResult(syncedAt, status);
        vehicleSyncConfigJpaRepository.save(entity);
    }

    private VehicleSyncConfigJpaEntity findOrCreateDefault() {
        return vehicleSyncConfigJpaRepository.findById(VehicleSyncConfigJpaEntity.DEFAULT_ID)
                .orElseGet(VehicleSyncConfigJpaEntity::createDefault);
    }

}
