package com.sledenje.teren.workorder.infrastructure.persistence;

import com.sledenje.teren.workorder.domain.model.InstallerName;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "work_order_installers")
public class WorkOrderInstallerJpaEntity {

    @Id
    private String id;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private InstallerName name;

    private String otherText;

    protected WorkOrderInstallerJpaEntity() {
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
