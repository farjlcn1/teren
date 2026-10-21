package com.sledenje.teren.workorder.infrastructure.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Immutable;

/**
 * Stranka, kot jo bere kontekst nalogov (samo za branje). Tabelo upravlja kontekst client.
 */
@Entity
@Immutable
@BatchSize(size = 100)
@Table(name = "clients")
public class WorkOrderClientJpaEntity {

    @Id
    private String id;

    private String name;

    private String address;

    private String contactInfo;

    protected WorkOrderClientJpaEntity() {
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public String getContactInfo() {
        return contactInfo;
    }

}
