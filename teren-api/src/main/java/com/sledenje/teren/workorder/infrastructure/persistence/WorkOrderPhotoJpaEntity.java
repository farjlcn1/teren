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
@Table(name = "work_order_photos")
public class WorkOrderPhotoJpaEntity {

    @Id
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workOrderId", nullable = false)
    private WorkOrderJpaEntity workOrder;

    @Column(nullable = false)
    private String filePath;

    private Instant takenAt;

    @Column(nullable = false)
    private Instant uploadedAt;

    protected WorkOrderPhotoJpaEntity() {
    }

    public WorkOrderPhotoJpaEntity(String id, WorkOrderJpaEntity workOrder, String filePath, Instant takenAt,
            Instant uploadedAt) {
        this.id = id;
        this.workOrder = workOrder;
        this.filePath = filePath;
        this.takenAt = takenAt;
        this.uploadedAt = uploadedAt;
    }

    public String getId() {
        return id;
    }

    public String getFilePath() {
        return filePath;
    }

    public Instant getTakenAt() {
        return takenAt;
    }

    public Instant getUploadedAt() {
        return uploadedAt;
    }

}
