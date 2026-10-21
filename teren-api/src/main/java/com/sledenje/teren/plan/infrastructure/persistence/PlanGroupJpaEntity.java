package com.sledenje.teren.plan.infrastructure.persistence;

import com.sledenje.teren.common.domain.model.InstallerName;
import com.sledenje.teren.common.infrastructure.persistence.TimestampedJpaEntity;
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

@Entity
@Table(name = "plan_groups")
public class PlanGroupJpaEntity extends TimestampedJpaEntity {

    @Id
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "clientId", nullable = false)
    private PlanClientJpaEntity client;

    @Column(nullable = false)
    private Instant startAt;

    @Column(nullable = false)
    private Instant endAt;

    private String note;

    private String contact;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private InstallerName expectedInstaller;

    private String expectedInstallerOtherText;

    @Column(nullable = false)
    private String createdById;

    @OneToMany(mappedBy = "planGroup", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    @BatchSize(size = 100)
    private List<PlannedTaskJpaEntity> tasks = new ArrayList<>();

    protected PlanGroupJpaEntity() {
    }

    public PlanGroupJpaEntity(String id, PlanClientJpaEntity client, Instant startAt, Instant endAt, String note,
            String contact, InstallerName expectedInstaller, String expectedInstallerOtherText, String createdById) {
        this.id = id;
        this.client = client;
        this.startAt = startAt;
        this.endAt = endAt;
        this.note = note;
        this.contact = contact;
        this.expectedInstaller = expectedInstaller;
        this.expectedInstallerOtherText = expectedInstallerOtherText;
        this.createdById = createdById;
    }

    public void update(PlanClientJpaEntity client, Instant startAt, Instant endAt, String note, String contact,
            InstallerName expectedInstaller, String expectedInstallerOtherText) {
        this.client = client;
        this.startAt = startAt;
        this.endAt = endAt;
        this.note = note;
        this.contact = contact;
        this.expectedInstaller = expectedInstaller;
        this.expectedInstallerOtherText = expectedInstallerOtherText;
    }

    public void changeWindow(Instant startAt, Instant endAt) {
        this.startAt = startAt;
        this.endAt = endAt;
    }

    @Override
    public String getId() {
        return id;
    }

    public PlanClientJpaEntity getClient() {
        return client;
    }

    public Instant getStartAt() {
        return startAt;
    }

    public Instant getEndAt() {
        return endAt;
    }

    public String getNote() {
        return note;
    }

    public String getContact() {
        return contact;
    }

    public InstallerName getExpectedInstaller() {
        return expectedInstaller;
    }

    public String getExpectedInstallerOtherText() {
        return expectedInstallerOtherText;
    }

    public String getCreatedById() {
        return createdById;
    }

    public List<PlannedTaskJpaEntity> getTasks() {
        return tasks;
    }

}
