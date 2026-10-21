package com.sledenje.teren.client.domain.repository;

import com.sledenje.teren.client.domain.model.ClientSyncConfig;
import java.time.Instant;
import java.util.Optional;

public interface ClientSyncConfigRepository {

    Optional<ClientSyncConfig> findSyncConfig();

    ClientSyncConfig saveConnection(String apiUrl, String apiToken);

    void saveSyncResult(Instant syncedAt, String status);

}
