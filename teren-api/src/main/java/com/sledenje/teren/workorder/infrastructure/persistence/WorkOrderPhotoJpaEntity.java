package com.sledenje.teren.workorder.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "work_order_photos")
public class WorkOrderPhotoJpaEntity {

    @Id
    private String id;

    @Column(nullable = false)
    private String filePath;

    private Instant takenAt;

    @Column(nullable = false)
    private Instant uploadedAt;

    protected WorkOrderPhotoJpaEntity() {
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
