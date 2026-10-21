package com.sledenje.teren.vehicle.infrastructure.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VehicleJpaRepository extends JpaRepository<VehicleJpaEntity, String> {

    List<VehicleJpaEntity> findAllByOrderByPlateAsc();

    Optional<VehicleJpaEntity> findByPlate(String plate);

    Optional<VehicleJpaEntity> findByExternalId(String externalId);

    // ON CONFLICT: hkratni vnos iste registrske ne prekine transakcije naloga.
    @Modifying
    @Query(value = """
            INSERT INTO "vehicles" ("id", "plate", "source", "createdAt", "updatedAt")
            VALUES (:id, :plate, CAST(:source AS "ClientSource"), :now, :now)
            ON CONFLICT ("plate") DO NOTHING
            """, nativeQuery = true)
    void insertIfPlateMissing(@Param("id") String id, @Param("plate") String plate, @Param("source") String source,
            @Param("now") Instant now);

}
