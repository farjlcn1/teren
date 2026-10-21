package com.sledenje.teren.plan.infrastructure.persistence;

import com.sledenje.teren.common.domain.model.WorkOrderType;
import com.sledenje.teren.common.infrastructure.persistence.TimestampedJpaEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "planned_tasks")
public class PlannedTaskJpaEntity extends TimestampedJpaEntity {

    @Id
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "planGroupId", nullable = false)
    private PlanGroupJpaEntity planGroup;

    @Column(nullable = false)
    private String vehiclePlate;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private WorkOrderType type;

    private String note;

    // Tuji ključ na work_orders (ON DELETE SET NULL); kontekst plana potrebuje samo ID.
    private String workOrderId;

    protected PlannedTaskJpaEntity() {
    }

    public PlannedTaskJpaEntity(String id, PlanGroupJpaEntity planGroup, String vehiclePlate, WorkOrderType type,
            String note, String workOrderId) {
        this.id = id;
        this.planGroup = planGroup;
        this.vehiclePlate = vehiclePlate;
        this.type = type;
        this.note = note;
        this.workOrderId = workOrderId;
    }

    @Override
    public String getId() {
        return id;
    }

    public PlanGroupJpaEntity getPlanGroup() {
        return planGroup;
    }

    public String getVehiclePlate() {
        return vehiclePlate;
    }

    public WorkOrderType getType() {
        return type;
    }

    public String getNote() {
        return note;
    }

    public String getWorkOrderId() {
        return workOrderId;
    }

}
