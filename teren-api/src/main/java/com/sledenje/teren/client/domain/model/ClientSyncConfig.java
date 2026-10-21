package com.sledenje.teren.client.domain.model;

import java.time.Instant;

/**
 * Nastavitve sinhronizacije strank z zunanjim sistemom in izid zadnjega zagona.
 *
 * @param apiUrl         URL, ki z GET zahtevo vrne JSON seznam strank; {@code null}, če povezava ni nastavljena
 * @param apiToken       Bearer žeton za zunanji sistem; nikoli ne zapusti API-ja
 * @param lastSyncStatus kratek opis izida zadnje sinhronizacije, namenjen administratorju
 */
public record ClientSyncConfig(String apiUrl, String apiToken, Instant lastSyncAt, String lastSyncStatus) {

    public static final ClientSyncConfig NOT_CONFIGURED = new ClientSyncConfig(null, null, null, null);

    public boolean isConfigured() {
        return apiUrl != null;
    }

    public boolean hasToken() {
        return apiToken != null;
    }

}
