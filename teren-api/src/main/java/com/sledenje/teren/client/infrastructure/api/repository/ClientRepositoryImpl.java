package com.sledenje.teren.client.infrastructure.api.repository;

import com.sledenje.teren.client.domain.model.Client;
import com.sledenje.teren.client.domain.model.ClientSource;
import com.sledenje.teren.client.domain.model.ClientUpdate;
import com.sledenje.teren.client.domain.model.NewClient;
import com.sledenje.teren.client.domain.model.SyncedClient;
import com.sledenje.teren.client.domain.repository.ClientRepository;
import com.sledenje.teren.client.infrastructure.persistence.ClientJpaEntity;
import com.sledenje.teren.client.infrastructure.persistence.ClientJpaRepository;
import com.sledenje.teren.client.infrastructure.util.ClientJpaConverter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class ClientRepositoryImpl implements ClientRepository {

    private final ClientJpaRepository clientJpaRepository;

    public ClientRepositoryImpl(ClientJpaRepository clientJpaRepository) {
        this.clientJpaRepository = clientJpaRepository;
    }

    @Override
    public List<Client> findAllClientsOrderedByName() {
        return clientJpaRepository.findAllByOrderByNameAsc().stream()
                .map(ClientJpaConverter::toDomain)
                .toList();
    }

    @Override
    public Optional<Client> findClientById(String clientId) {
        return clientJpaRepository.findById(clientId).map(ClientJpaConverter::toDomain);
    }

    @Override
    public boolean existsClientById(String clientId) {
        return clientJpaRepository.existsById(clientId);
    }

    @Override
    public boolean isClientReferenced(String clientId) {
        return clientJpaRepository.isReferencedByWorkOrdersOrPlanGroups(clientId);
    }

    @Override
    public Client saveNewClient(NewClient newClient, ClientSource source) {
        ClientJpaEntity savedEntity = clientJpaRepository.save(
                ClientJpaConverter.toNewEntity(newClientId(), newClient, source));
        return ClientJpaConverter.toDomain(savedEntity);
    }

    @Override
    public int saveNewClients(List<NewClient> newClients, ClientSource source) {
        List<ClientJpaEntity> newEntities = newClients.stream()
                .map(newClient -> ClientJpaConverter.toNewEntity(newClientId(), newClient, source))
                .toList();
        return clientJpaRepository.saveAll(newEntities).size();
    }

    @Override
    public Optional<Client> updateClient(String clientId, ClientUpdate clientUpdate) {
        return clientJpaRepository.findById(clientId).map(entity -> {
            entity.updateDetails(clientUpdate.name(), clientUpdate.address(), clientUpdate.contactInfo());
            // Flush sproži @PreUpdate, da vrnjena stranka že vsebuje nov updatedAt.
            return ClientJpaConverter.toDomain(clientJpaRepository.saveAndFlush(entity));
        });
    }

    @Override
    public void deleteClientById(String clientId) {
        clientJpaRepository.deleteById(clientId);
    }

    @Override
    @Transactional
    public void upsertSyncedClients(List<SyncedClient> syncedClients) {
        for (SyncedClient syncedClient : syncedClients) {
            clientJpaRepository.findByExternalId(syncedClient.externalId()).ifPresentOrElse(
                    existingEntity -> existingEntity.updateDetails(
                            syncedClient.name(), syncedClient.address(), syncedClient.contactInfo()),
                    () -> clientJpaRepository.save(
                            ClientJpaConverter.toNewSyncedEntity(newClientId(), syncedClient)));
        }
    }

    private static String newClientId() {
        // Obstoječi zapisi imajo cuid ID-je (Prisma), novi dobijo UUID; stolpec je TEXT in format ni preverjan.
        return UUID.randomUUID().toString();
    }

}
