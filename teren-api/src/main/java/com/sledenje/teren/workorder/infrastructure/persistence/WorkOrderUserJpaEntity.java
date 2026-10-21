package com.sledenje.teren.workorder.infrastructure.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Immutable;

/**
 * Uporabnik, kot ga bere kontekst nalogov (samo ime, samo za branje). Tabelo upravlja kontekst user.
 */
@Entity
@Immutable
@BatchSize(size = 100)
@Table(name = "users")
public class WorkOrderUserJpaEntity {

    @Id
    private String id;

    private String fullName;

    protected WorkOrderUserJpaEntity() {
    }

    public String getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

}
