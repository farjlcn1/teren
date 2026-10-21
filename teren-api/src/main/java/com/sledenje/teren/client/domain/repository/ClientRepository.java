package com.sledenje.teren.client.domain.repository;

import com.sledenje.teren.client.domain.model.Client;
import com.sledenje.teren.client.domain.model.ClientSource;
import com.sledenje.teren.client.domain.model.NewClient;
import java.util.List;
import java.util.Optional;

public interface ClientRepository {

    List<Client> findAllClientsOrderedByName();

    Optional<Client> findClientById(String clientId);

    Client saveNewClient(NewClient newClient, ClientSource source);

}
