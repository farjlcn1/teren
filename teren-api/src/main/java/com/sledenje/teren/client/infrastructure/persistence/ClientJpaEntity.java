package com.sledenje.teren.client.infrastructure.persistence;

import com.sledenje.teren.client.domain.model.ClientSource;
import com.sledenje.teren.common.infrastructure.persistence.TimestampedJpaEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "clients")
public class ClientJpaEntity extends TimestampedJpaEntity {

    @Id
    private String id;

    @Column(nullable = false)
    private String name;

    private String address;

    private String contactInfo;

    // Stolpec je Postgres enum tip "ClientSource", ne VARCHAR.
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private ClientSource source;

    private String externalId;

    protected ClientJpaEntity() {
    }

    public ClientJpaEntity(String id, String name, String address, String contactInfo, ClientSource source) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.contactInfo = contactInfo;
        this.source = source;
    }

    @Override
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

    public ClientSource getSource() {
        return source;
    }

    public String getExternalId() {
        return externalId;
    }

}
