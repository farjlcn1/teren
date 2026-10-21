package com.sledenje.teren.client.infrastructure.util;

import com.sledenje.teren.client.domain.model.Client;
import com.sledenje.teren.client.domain.model.ClientSource;
import com.sledenje.teren.client.domain.model.NewClient;
import com.sledenje.teren.client.infrastructure.persistence.ClientJpaEntity;

/**
 * Pretvorbe med JPA entiteto in domenskim modelom stranke.
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

}
