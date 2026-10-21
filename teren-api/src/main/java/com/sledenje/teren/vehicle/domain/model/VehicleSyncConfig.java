package com.sledenje.teren.vehicle.domain.model;

import java.time.Instant;

/**
 * Nastavitve sinhronizacije vozil z zunanjim sistemom in izid zadnjega zagona.
 *
 * @param apiUrl         URL, ki z GET zahtevo vrne JSON seznam vozil; {@code null}, če povezava ni nastavljena
 * @param apiToken       Bearer žeton za zunanji sistem; nikoli ne zapusti API-ja
 * @param lastSyncStatus kratek opis izida zadnje sinhronizacije, namenjen administratorju
 */
public record VehicleSyncConfig(String apiUrl, String apiToken, Instant lastSyncAt, String lastSyncStatus) {

    public static final VehicleSyncConfig NOT_CONFIGURED = new VehicleSyncConfig(null, null, null, null);

    public boolean isConfigured() {
        return apiUrl != null;
    }

    public boolean hasToken() {
        return apiToken != null;
    }

}
