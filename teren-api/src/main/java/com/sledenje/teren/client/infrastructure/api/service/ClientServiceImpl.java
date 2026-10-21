package com.sledenje.teren.client.infrastructure.api.service;

import com.sledenje.teren.client.domain.exception.ClientInUseException;
import com.sledenje.teren.client.domain.exception.ClientNotFoundException;
import com.sledenje.teren.client.domain.model.Client;
import com.sledenje.teren.client.domain.model.ClientSource;
import com.sledenje.teren.client.domain.model.ClientUpdate;
import com.sledenje.teren.client.domain.model.NewClient;
import com.sledenje.teren.client.domain.repository.ClientRepository;
import com.sledenje.teren.client.domain.service.ClientService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClientServiceImpl implements ClientService {

    private static final Logger log = LoggerFactory.getLogger(ClientServiceImpl.class);

    private final ClientRepository clientRepository;

    public ClientServiceImpl(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Client> findAllClients() {
        return clientRepository.findAllClientsOrderedByName();
    }

    @Override
    @Transactional(readOnly = true)
    public Client findClientById(String clientId) {
        return clientRepository.findClientById(clientId)
                .orElseThrow(() -> new ClientNotFoundException(clientId));
    }

    @Override
    @Transactional
    public Client createClient(NewClient newClient) {
        Client createdClient = clientRepository.saveNewClient(newClient, ClientSource.MANUAL);
        log.info("Dodana stranka {}", createdClient.id());
        return createdClient;
    }

    @Override
    @Transactional
    public int importClients(List<NewClient> newClients) {
        int importedCount = clientRepository.saveNewClients(newClients, ClientSource.IMPORT);
        log.info("Uvoženih {} strank", importedCount);
        return importedCount;
    }

    @Override
    @Transactional
    public Client updateClient(String clientId, ClientUpdate clientUpdate) {
        Client updatedClient = clientRepository.updateClient(clientId, clientUpdate)
                .orElseThrow(() -> new ClientNotFoundException(clientId));
        log.info("Posodobljena stranka {}", clientId);
        return updatedClient;
    }

    @Override
    @Transactional
    public void deleteClient(String clientId) {
        if (!clientRepository.existsClientById(clientId)) {
            throw new ClientNotFoundException(clientId);
        }
        if (clientRepository.isClientReferenced(clientId)) {
            throw new ClientInUseException();
        }
        clientRepository.deleteClientById(clientId);
        log.info("Izbrisana stranka {}", clientId);
    }

}
