package com.sledenje.teren.client.domain.repository;

import com.sledenje.teren.client.domain.exception.ClientSyncFailedException;
import com.sledenje.teren.client.domain.model.SyncedClient;
import java.util.List;

/**
 * Stranke iz zunanjega sistema.
 */
public interface ExternalClientRepository {

    /**
     * @param apiToken Bearer žeton ali {@code null}, če ga zunanji sistem ne zahteva
     * @throws ClientSyncFailedException če zunanji sistem ni dosegljiv ali vrne neveljaven odgovor
     */
    List<SyncedClient> fetchClients(String apiUrl, String apiToken);

}
