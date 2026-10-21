package com.sledenje.teren.workorder.infrastructure.persistence;

import com.sledenje.teren.workorder.domain.model.OptionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "work_order_options")
public class WorkOrderOptionJpaEntity {

    @Id
    private String id;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private OptionType optionType;

    private String comment;

    protected WorkOrderOptionJpaEntity() {
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
