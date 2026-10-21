package com.sledenje.teren.vehicle.infrastructure.api.repository;

import com.sledenje.teren.vehicle.domain.exception.VehiclePlateAlreadyExistsException;
import com.sledenje.teren.vehicle.domain.model.SyncedVehicle;
import com.sledenje.teren.vehicle.domain.model.Vehicle;
import com.sledenje.teren.vehicle.domain.model.VehicleSource;
import com.sledenje.teren.vehicle.domain.repository.VehicleRepository;
import com.sledenje.teren.vehicle.infrastructure.persistence.VehicleJpaEntity;
import com.sledenje.teren.vehicle.infrastructure.persistence.VehicleJpaRepository;
import com.sledenje.teren.vehicle.infrastructure.util.VehicleJpaConverter;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class VehicleRepositoryImpl implements VehicleRepository {

    private final VehicleJpaRepository vehicleJpaRepository;

    public VehicleRepositoryImpl(VehicleJpaRepository vehicleJpaRepository) {
        this.vehicleJpaRepository = vehicleJpaRepository;
    }

    @Override
    public List<Vehicle> findAllVehiclesOrderedByPlate() {
        return vehicleJpaRepository.findAllByOrderByPlateAsc().stream()
                .map(VehicleJpaConverter::toDomain)
                .toList();
    }

    @Override
    public Optional<Vehicle> findVehicleById(String vehicleId) {
        return vehicleJpaRepository.findById(vehicleId).map(VehicleJpaConverter::toDomain);
    }

    @Override
    public Optional<Vehicle> findVehicleByPlate(String plate) {
        return vehicleJpaRepository.findByPlate(plate).map(VehicleJpaConverter::toDomain);
    }

    @Override
    public boolean existsVehicleById(String vehicleId) {
        return vehicleJpaRepository.existsById(vehicleId);
    }

    @Override
    public Vehicle saveNewVehicle(String plate, VehicleSource source) {
        VehicleJpaEntity savedEntity = vehicleJpaRepository.save(
                new VehicleJpaEntity(newVehicleId(), plate, source, null));
        return VehicleJpaConverter.toDomain(savedEntity);
    }

    @Override
    public Optional<Vehicle> updateVehiclePlate(String vehicleId, String plate) {
        return vehicleJpaRepository.findById(vehicleId).map(entity -> {
            entity.changePlate(plate);
            // Flush sproži @PreUpdate, da vrnjeno vozilo že vsebuje nov updatedAt.
            return VehicleJpaConverter.toDomain(vehicleJpaRepository.saveAndFlush(entity));
        });
    }

    @Override
    public void deleteVehicleById(String vehicleId) {
        vehicleJpaRepository.deleteById(vehicleId);
    }

    @Override
    public void insertVehicleIfMissing(String plate, VehicleSource source) {
        vehicleJpaRepository.insertIfPlateMissing(newVehicleId(), plate, source.name(),
                Instant.now().truncatedTo(ChronoUnit.MILLIS));
    }

    @Override
    @Transactional
    public void upsertSyncedVehicles(List<SyncedVehicle> syncedVehicles) {
        for (SyncedVehicle syncedVehicle : syncedVehicles) {
            Optional<VehicleJpaEntity> knownEntity = vehicleJpaRepository.findByExternalId(syncedVehicle.externalId());
            String knownEntityId = knownEntity.map(VehicleJpaEntity::getId).orElse(null);
            boolean plateTakenByOtherVehicle = vehicleJpaRepository.findByPlate(syncedVehicle.plate())
                    .filter(entityWithPlate -> !entityWithPlate.getId().equals(knownEntityId))
                    .isPresent();
            if (plateTakenByOtherVehicle) {
                throw new VehiclePlateAlreadyExistsException(syncedVehicle.plate());
            }
            knownEntity.ifPresentOrElse(
                    entity -> entity.changePlate(syncedVehicle.plate()),
                    () -> vehicleJpaRepository.save(new VehicleJpaEntity(
                            newVehicleId(), syncedVehicle.plate(), VehicleSource.SYNC, syncedVehicle.externalId())));
        }
    }

    private static String newVehicleId() {
        // Obstoječi zapisi imajo cuid ID-je (Prisma), novi dobijo UUID; stolpec je TEXT in format ni preverjan.
        return UUID.randomUUID().toString();
    }

}
