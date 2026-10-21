package com.sledenje.teren.client.infrastructure.util;

import com.sledenje.teren.client.domain.model.Client;
import com.sledenje.teren.client.domain.model.ClientSource;
import com.sledenje.teren.client.domain.model.ClientSyncConfig;
import com.sledenje.teren.client.domain.model.NewClient;
import com.sledenje.teren.client.domain.model.SyncedClient;
import com.sledenje.teren.client.infrastructure.persistence.ClientJpaEntity;
import com.sledenje.teren.client.infrastructure.persistence.ClientSyncConfigJpaEntity;

/**
 * Pretvorbe med JPA entitetami in domenskim modelom stranke.
 */
public final class ClientJpaConverter {

    private ClientJpaConverter() {
    }

    public static Client toDomain(ClientJpaEntity entity) {
        return new Client(
                entity.getId(),
                entity.getName(),
                entity.getAddress(),
                entity.getContactInfo(),
                entity.getSource(),
                entity.getExternalId(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    public static ClientJpaEntity toNewEntity(String clientId, NewClient newClient, ClientSource source) {
        return new ClientJpaEntity(clientId, newClient.name(), newClient.address(), newClient.contactInfo(), source);
    }

    public static ClientJpaEntity toNewSyncedEntity(String clientId, SyncedClient syncedClient) {
        return new ClientJpaEntity(clientId, syncedClient.name(), syncedClient.address(), syncedClient.contactInfo(),
                ClientSource.SYNC, syncedClient.externalId());
    }

    public static ClientSyncConfig toDomain(ClientSyncConfigJpaEntity entity) {
        return new ClientSyncConfig(
                entity.getApiUrl(),
                entity.getApiToken(),
                entity.getLastSyncAt(),
                entity.getLastSyncStatus());
    }

}
