package com.sledenje.teren.client.infrastructure.api.repository;

import com.sledenje.teren.client.domain.model.Client;
import com.sledenje.teren.client.domain.model.ClientSource;
import com.sledenje.teren.client.domain.model.NewClient;
import com.sledenje.teren.client.domain.repository.ClientRepository;
import com.sledenje.teren.client.infrastructure.persistence.ClientJpaEntity;
import com.sledenje.teren.client.infrastructure.persistence.ClientJpaRepository;
import com.sledenje.teren.client.infrastructure.util.ClientJpaConverter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

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
    public Client saveNewClient(NewClient newClient, ClientSource source) {
        // Obstoječi zapisi imajo cuid ID-je (Prisma), novi dobijo UUID; stolpec je TEXT in format ni preverjan.
        String clientId = UUID.randomUUID().toString();
        ClientJpaEntity savedEntity = clientJpaRepository.save(
                ClientJpaConverter.toNewEntity(clientId, newClient, source));
        return ClientJpaConverter.toDomain(savedEntity);
    }

}
