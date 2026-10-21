package com.sledenje.teren.client.domain.service;

import com.sledenje.teren.client.domain.exception.ClientNotFoundException;
import com.sledenje.teren.client.domain.model.Client;
import com.sledenje.teren.client.domain.model.NewClient;
import java.util.List;

public interface ClientService {

    /**
     * @return vse stranke, urejene po imenu
     */
    List<Client> findAllClients();

    /**
     * @param clientId ID stranke
     * @return stranka z danim ID-jem
     * @throws ClientNotFoundException če stranka s tem ID-jem ne obstaja
     */
    Client findClientById(String clientId);

    /**
     * Ročno doda novo stranko (izvor {@code MANUAL}).
     *
     * @param newClient podatki nove stranke
     * @return shranjena stranka z dodeljenim ID-jem
     */
    Client createClient(NewClient newClient);

}
