package com.sledenje.teren.client.infrastructure.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Edina vrstica tabele {@code client_sync_config} z ID-jem {@value #DEFAULT_ID} (tako jo uporablja Prisma).
 */
@Entity
@Table(name = "client_sync_config")
public class ClientSyncConfigJpaEntity {

    public static final String DEFAULT_ID = "default";

    @Id
    private String id;

    private String apiUrl;

    private String apiToken;

    private Instant lastSyncAt;

    private String lastSyncStatus;

    protected ClientSyncConfigJpaEntity() {
    }

    public static ClientSyncConfigJpaEntity createDefault() {
        ClientSyncConfigJpaEntity entity = new ClientSyncConfigJpaEntity();
        entity.id = DEFAULT_ID;
        return entity;
    }

    public void updateConnection(String apiUrl, String apiToken) {
        this.apiUrl = apiUrl;
        this.apiToken = apiToken;
    }

    public void recordSyncResult(Instant syncedAt, String status) {
        this.lastSyncAt = syncedAt;
        this.lastSyncStatus = status;
    }

    public String getId() {
        return id;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public String getApiToken() {
        return apiToken;
    }

    public Instant getLastSyncAt() {
        return lastSyncAt;
    }

    public String getLastSyncStatus() {
        return lastSyncStatus;
    }

}
