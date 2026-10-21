package com.sledenje.teren.workorder.infrastructure.persistence;

import com.sledenje.teren.common.domain.model.InstallerName;
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
@Table(name = "work_order_installers")
public class WorkOrderInstallerJpaEntity {

    @Id
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workOrderId", nullable = false)
    private WorkOrderJpaEntity workOrder;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private InstallerName name;

    private String otherText;

    protected WorkOrderInstallerJpaEntity() {
    }

    public WorkOrderInstallerJpaEntity(String id, WorkOrderJpaEntity workOrder, InstallerName name, String otherText) {
        this.id = id;
        this.workOrder = workOrder;
        this.name = name;
        this.otherText = otherText;
    }

    public String getId() {
        return id;
    }

    public InstallerName getName() {
        return name;
    }

    public String getOtherText() {
        return otherText;
    }

}
