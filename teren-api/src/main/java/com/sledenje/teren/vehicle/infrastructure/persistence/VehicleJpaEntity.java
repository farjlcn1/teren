package com.sledenje.teren.vehicle.infrastructure.persistence;

import com.sledenje.teren.common.infrastructure.persistence.TimestampedJpaEntity;
import com.sledenje.teren.vehicle.domain.model.VehicleSource;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "vehicles")
public class VehicleJpaEntity extends TimestampedJpaEntity {

    @Id
    private String id;

    @Column(nullable = false)
    private String plate;

    // Stolpec je Postgres enum tip "ClientSource" (skupen s strankami), ne VARCHAR.
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private VehicleSource source;

    private String externalId;

    protected VehicleJpaEntity() {
    }

    public VehicleJpaEntity(String id, String plate, VehicleSource source, String externalId) {
        this.id = id;
        this.plate = plate;
        this.source = source;
        this.externalId = externalId;
    }

    public void changePlate(String plate) {
        this.plate = plate;
    }

    @Override
    public String getId() {
        return id;
    }

    public String getPlate() {
        return plate;
    }

    public VehicleSource getSource() {
        return source;
    }

    public String getExternalId() {
        return externalId;
    }

}
