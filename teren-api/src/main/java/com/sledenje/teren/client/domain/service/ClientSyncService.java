package com.sledenje.teren.client.domain.service;

import com.sledenje.teren.client.domain.exception.ClientSyncFailedException;
import com.sledenje.teren.client.domain.exception.ClientSyncNotConfiguredException;
import com.sledenje.teren.client.domain.model.ClientSyncConfig;

public interface ClientSyncService {

    /**
     * @return trenutne nastavitve; {@link ClientSyncConfig#NOT_CONFIGURED}, če še niso bile shranjene
     */
    ClientSyncConfig findSyncConfig();

    /**
     * Shrani povezavo do zunanjega sistema. Prazen {@code apiToken} obdrži obstoječi žeton, če se URL ne spremeni
     * (ob novem URL-ju je brez žetona); prazen {@code apiUrl} izbriše povezavo skupaj z žetonom.
     */
    ClientSyncConfig updateSyncConfig(String apiUrl, String apiToken);

    /**
     * Prenese stranke iz zunanjega sistema in izid zapiše v nastavitve.
     *
     * @return število sinhroniziranih strank
     * @throws ClientSyncNotConfiguredException če povezava ni nastavljena
     * @throws ClientSyncFailedException        če zunanji sistem ni odgovoril pravilno
     */
    int syncClients();

}
