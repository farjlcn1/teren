package com.sledenje.teren.client.infrastructure.api.repository;

import com.sledenje.teren.client.domain.model.ClientSyncConfig;
import com.sledenje.teren.client.domain.repository.ClientSyncConfigRepository;
import com.sledenje.teren.client.infrastructure.persistence.ClientSyncConfigJpaEntity;
import com.sledenje.teren.client.infrastructure.persistence.ClientSyncConfigJpaRepository;
import com.sledenje.teren.client.infrastructure.util.ClientJpaConverter;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class ClientSyncConfigRepositoryImpl implements ClientSyncConfigRepository {

    private final ClientSyncConfigJpaRepository clientSyncConfigJpaRepository;

    public ClientSyncConfigRepositoryImpl(ClientSyncConfigJpaRepository clientSyncConfigJpaRepository) {
        this.clientSyncConfigJpaRepository = clientSyncConfigJpaRepository;
    }

    @Override
    public Optional<ClientSyncConfig> findSyncConfig() {
        return clientSyncConfigJpaRepository.findById(ClientSyncConfigJpaEntity.DEFAULT_ID)
                .map(ClientJpaConverter::toDomain);
    }

    @Override
    @Transactional
    public ClientSyncConfig saveConnection(String apiUrl, String apiToken) {
        ClientSyncConfigJpaEntity entity = findOrCreateDefault();
        entity.updateConnection(apiUrl, apiToken);
        return ClientJpaConverter.toDomain(clientSyncConfigJpaRepository.save(entity));
    }

    @Override
    @Transactional
    public void saveSyncResult(Instant syncedAt, String status) {
        ClientSyncConfigJpaEntity entity = findOrCreateDefault();
        entity.recordSyncResult(syncedAt, status);
        clientSyncConfigJpaRepository.save(entity);
    }

    private ClientSyncConfigJpaEntity findOrCreateDefault() {
        return clientSyncConfigJpaRepository.findById(ClientSyncConfigJpaEntity.DEFAULT_ID)
                .orElseGet(ClientSyncConfigJpaEntity::createDefault);
    }

}
