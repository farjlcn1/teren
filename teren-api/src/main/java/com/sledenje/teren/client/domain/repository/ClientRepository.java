package com.sledenje.teren.client.domain.repository;

import com.sledenje.teren.client.domain.model.Client;
import com.sledenje.teren.client.domain.model.ClientSource;
import com.sledenje.teren.client.domain.model.ClientUpdate;
import com.sledenje.teren.client.domain.model.NewClient;
import com.sledenje.teren.client.domain.model.SyncedClient;
import java.util.List;
import java.util.Optional;

public interface ClientRepository {

    List<Client> findAllClientsOrderedByName();

    Optional<Client> findClientById(String clientId);

    boolean existsClientById(String clientId);

    /**
     * @return {@code true}, če se na stranko sklicuje vsaj en delovni nalog ali dogodek v planu
     */
    boolean isClientReferenced(String clientId);

    Client saveNewClient(NewClient newClient, ClientSource source);

    /**
     * @return število shranjenih strank
     */
    int saveNewClients(List<NewClient> newClients, ClientSource source);

    /**
     * @return posodobljena stranka ali prazno, če stranka s tem ID-jem ne obstaja
     */
    Optional<Client> updateClient(String clientId, ClientUpdate clientUpdate);

    void deleteClientById(String clientId);

    /**
     * Stranke z znanim {@code externalId} posodobi, ostale doda z izvorom {@code SYNC}. Vse v eni transakciji.
     */
    void upsertSyncedClients(List<SyncedClient> syncedClients);

}
