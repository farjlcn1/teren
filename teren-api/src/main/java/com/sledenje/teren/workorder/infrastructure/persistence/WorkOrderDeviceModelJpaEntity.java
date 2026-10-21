package com.sledenje.teren.workorder.infrastructure.persistence;

import com.sledenje.teren.workorder.domain.model.DeviceModel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "work_order_device_models")
public class WorkOrderDeviceModelJpaEntity {

    @Id
    private String id;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private DeviceModel deviceModel;

    private String comment;

    protected WorkOrderDeviceModelJpaEntity() {
    }

    public String getId() {
        return id;
    }

    public DeviceModel getDeviceModel() {
        return deviceModel;
    }

    public String getComment() {
        return comment;
    }

}
