package com.sledenje.teren.workorder.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "work_order_edits")
public class WorkOrderEditJpaEntity {

    @Id
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workOrderId", nullable = false)
    private WorkOrderJpaEntity workOrder;

    @Column(nullable = false)
    private Instant editedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "editedById", nullable = false)
    private WorkOrderUserJpaEntity editedBy;

    @Column(nullable = false)
    private String changesJson;

    protected WorkOrderEditJpaEntity() {
    }

    public WorkOrderEditJpaEntity(String id, WorkOrderJpaEntity workOrder, Instant editedAt,
            WorkOrderUserJpaEntity editedBy, String changesJson) {
        this.id = id;
        this.workOrder = workOrder;
        this.editedAt = editedAt;
        this.editedBy = editedBy;
        this.changesJson = changesJson;
    }

    public String getId() {
        return id;
    }

    public Instant getEditedAt() {
        return editedAt;
    }

    public WorkOrderUserJpaEntity getEditedBy() {
        return editedBy;
    }

    public String getChangesJson() {
        return changesJson;
    }

}
