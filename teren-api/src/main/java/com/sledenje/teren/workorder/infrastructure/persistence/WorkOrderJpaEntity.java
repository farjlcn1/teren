package com.sledenje.teren.workorder.infrastructure.persistence;

import com.sledenje.teren.common.domain.model.WorkOrderType;
import com.sledenje.teren.common.infrastructure.persistence.TimestampedJpaEntity;
import com.sledenje.teren.workorder.domain.model.Culprit;
import com.sledenje.teren.workorder.domain.model.Difficulty;
import com.sledenje.teren.workorder.domain.model.WorkOrderStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Delovni nalog. Monterji, opcije in modeli naprav se naložijo v paketih ({@link BatchSize}),
 * da seznam nalogov ne sproži poizvedbe za vsak nalog posebej.
 */
@Entity
@Table(name = "work_orders")
public class WorkOrderJpaEntity extends TimestampedJpaEntity {

    private static final int BATCH_SIZE = 100;

    @Id
    private String id;

    @Column(nullable = false)
    private String ident;

    @Column(nullable = false)
    private Instant orderDate;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private WorkOrderType type;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private Difficulty difficulty;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private WorkOrderStatus status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "clientId", nullable = false)
    private WorkOrderClientJpaEntity client;

    @Column(nullable = false)
    private String vehiclePlate;

    @Column(nullable = false)
    private String vehicleBrand;

    @Column(nullable = false)
    private String vehicleModel;

    @Column(nullable = false)
    private int vehicleYear;

    @Column(nullable = false)
    private String imei;

    private String imeiPrev;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private Culprit culprit;

    private String comment;

    private String signatureUrl;

    private Instant signedAt;

    private Instant lockedAt;

    private Instant sentAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "createdById", nullable = false)
    private WorkOrderUserJpaEntity createdBy;

    // Vrstni red enumov v Postgresu je vrstni red v obrazcu.
    @OneToMany(mappedBy = "workOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("name")
    @BatchSize(size = BATCH_SIZE)
    private List<WorkOrderInstallerJpaEntity> installers = new ArrayList<>();

    @OneToMany(mappedBy = "workOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("optionType")
    @BatchSize(size = BATCH_SIZE)
    private List<WorkOrderOptionJpaEntity> options = new ArrayList<>();

    @OneToMany(mappedBy = "workOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("deviceModel")
    @BatchSize(size = BATCH_SIZE)
    private List<WorkOrderDeviceModelJpaEntity> deviceModels = new ArrayList<>();

    @OneToMany(mappedBy = "workOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("uploadedAt ASC, id ASC")
    private List<WorkOrderPhotoJpaEntity> photos = new ArrayList<>();

    @OneToMany(mappedBy = "workOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("editedAt DESC")
    private List<WorkOrderEditJpaEntity> edits = new ArrayList<>();

    protected WorkOrderJpaEntity() {
    }

    /**
     * Nov aktiven nalog brez podpisa; monterji, opcije in modeli naprav se dodajo v zbirke.
     */
    public WorkOrderJpaEntity(String id, String ident, Instant orderDate, WorkOrderUserJpaEntity createdBy) {
        this.id = id;
        this.ident = ident;
        this.orderDate = orderDate;
        this.createdBy = createdBy;
        this.status = WorkOrderStatus.ACTIVE;
    }

    public void updateDetails(WorkOrderType type, Difficulty difficulty, WorkOrderClientJpaEntity client,
            String vehiclePlate, String vehicleBrand, String vehicleModel, int vehicleYear, String imei,
            String imeiPrev, Culprit culprit, String comment) {
        this.type = type;
        this.difficulty = difficulty;
        this.client = client;
        this.vehiclePlate = vehiclePlate;
        this.vehicleBrand = vehicleBrand;
        this.vehicleModel = vehicleModel;
        this.vehicleYear = vehicleYear;
        this.imei = imei;
        this.imeiPrev = imeiPrev;
        this.culprit = culprit;
        this.comment = comment;
    }

    /**
     * Podpis zaklene nalog.
     */
    public void sign(String signatureUrl, Instant signedAt) {
        this.signatureUrl = signatureUrl;
        this.signedAt = signedAt;
        this.lockedAt = signedAt;
    }

    public void markSent(Instant sentAt) {
        this.sentAt = sentAt;
    }

    @Override
    public String getId() {
        return id;
    }

    public String getIdent() {
        return ident;
    }

    public Instant getOrderDate() {
        return orderDate;
    }

    public WorkOrderType getType() {
        return type;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public WorkOrderStatus getStatus() {
        return status;
    }

    public WorkOrderClientJpaEntity getClient() {
        return client;
    }

    public String getVehiclePlate() {
        return vehiclePlate;
    }

    public String getVehicleBrand() {
        return vehicleBrand;
    }

    public String getVehicleModel() {
        return vehicleModel;
    }

    public int getVehicleYear() {
        return vehicleYear;
    }

    public String getImei() {
        return imei;
    }

    public String getImeiPrev() {
        return imeiPrev;
    }

    public Culprit getCulprit() {
        return culprit;
    }

    public String getComment() {
        return comment;
    }

    public String getSignatureUrl() {
        return signatureUrl;
    }

    public Instant getSignedAt() {
        return signedAt;
    }

    public Instant getLockedAt() {
        return lockedAt;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public WorkOrderUserJpaEntity getCreatedBy() {
        return createdBy;
    }

    public List<WorkOrderInstallerJpaEntity> getInstallers() {
        return installers;
    }

    public List<WorkOrderOptionJpaEntity> getOptions() {
        return options;
    }

    public List<WorkOrderDeviceModelJpaEntity> getDeviceModels() {
        return deviceModels;
    }

    public List<WorkOrderPhotoJpaEntity> getPhotos() {
        return photos;
    }

    public List<WorkOrderEditJpaEntity> getEdits() {
        return edits;
    }

}
