package com.sledenje.teren.plan.infrastructure.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Immutable;

/**
 * Stranka, kot jo bere kontekst plana (samo ime, samo za branje). Tabelo upravlja kontekst client.
 */
@Entity
@Immutable
@BatchSize(size = 100)
@Table(name = "clients")
public class PlanClientJpaEntity {

    @Id
    private String id;

    private String name;

    protected PlanClientJpaEntity() {
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

}
