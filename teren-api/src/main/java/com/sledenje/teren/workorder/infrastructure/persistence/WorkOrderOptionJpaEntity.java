package com.sledenje.teren.workorder.infrastructure.persistence;

import com.sledenje.teren.workorder.domain.model.OptionType;
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
@Table(name = "work_order_options")
public class WorkOrderOptionJpaEntity {

    @Id
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workOrderId", nullable = false)
    private WorkOrderJpaEntity workOrder;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private OptionType optionType;

    private String comment;

    protected WorkOrderOptionJpaEntity() {
    }

    public WorkOrderOptionJpaEntity(String id, WorkOrderJpaEntity workOrder, OptionType optionType, String comment) {
        this.id = id;
        this.workOrder = workOrder;
        this.optionType = optionType;
        this.comment = comment;
    }

    public String getId() {
        return id;
    }

    public OptionType getOptionType() {
        return optionType;
    }

    public String getComment() {
        return comment;
    }

}
