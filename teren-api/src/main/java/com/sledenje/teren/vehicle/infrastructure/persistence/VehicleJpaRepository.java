package com.sledenje.teren.vehicle.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleJpaRepository extends JpaRepository<VehicleJpaEntity, String> {

    List<VehicleJpaEntity> findAllByOrderByPlateAsc();

    Optional<VehicleJpaEntity> findByPlate(String plate);

    Optional<VehicleJpaEntity> findByExternalId(String externalId);

}
