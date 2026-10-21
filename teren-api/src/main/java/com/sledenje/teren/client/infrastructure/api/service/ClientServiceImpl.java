package com.sledenje.teren.client.infrastructure.api.service;

import com.sledenje.teren.client.domain.exception.ClientNotFoundException;
import com.sledenje.teren.client.domain.model.Client;
import com.sledenje.teren.client.domain.model.ClientSource;
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

}
