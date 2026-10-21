package com.sledenje.teren.client.infrastructure.api.service;

import com.sledenje.teren.client.domain.exception.ClientSyncFailedException;
import com.sledenje.teren.client.domain.exception.ClientSyncNotConfiguredException;
import com.sledenje.teren.client.domain.model.ClientSyncConfig;
import com.sledenje.teren.client.domain.model.SyncedClient;
import com.sledenje.teren.client.domain.repository.ClientRepository;
import com.sledenje.teren.client.domain.repository.ClientSyncConfigRepository;
import com.sledenje.teren.client.domain.repository.ExternalClientRepository;
import com.sledenje.teren.client.domain.service.ClientSyncService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClientSyncServiceImpl implements ClientSyncService {

    private static final Logger log = LoggerFactory.getLogger(ClientSyncServiceImpl.class);

    private final ClientSyncConfigRepository clientSyncConfigRepository;
    private final ExternalClientRepository externalClientRepository;
    private final ClientRepository clientRepository;

    public ClientSyncServiceImpl(ClientSyncConfigRepository clientSyncConfigRepository,
            ExternalClientRepository externalClientRepository, ClientRepository clientRepository) {
        this.clientSyncConfigRepository = clientSyncConfigRepository;
        this.externalClientRepository = externalClientRepository;
        this.clientRepository = clientRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public ClientSyncConfig findSyncConfig() {
        return clientSyncConfigRepository.findSyncConfig().orElse(ClientSyncConfig.NOT_CONFIGURED);
    }

    @Override
    @Transactional
    public ClientSyncConfig updateSyncConfig(String apiUrl, String apiToken) {
        String newApiUrl = blankToNull(apiUrl);
        String newApiToken = blankToNull(apiToken);
        if (newApiUrl == null) {
            newApiToken = null;
        } else if (newApiToken == null && newApiUrl.equals(findSyncConfig().apiUrl())) {
            // Obrazec žetona ne prikaže; prazno polje obdrži obstoječega, a samo za isti URL. Ob novem URL-ju
            // bi sicer stari žeton odšel na drug strežnik (npr. napadalčev).
            newApiToken = findSyncConfig().apiToken();
        }
        ClientSyncConfig savedConfig = clientSyncConfigRepository.saveConnection(newApiUrl, newApiToken);
        log.info("Shranjene nastavitve sinhronizacije strank (povezava {})",
                savedConfig.isConfigured() ? "nastavljena" : "izbrisana");
        return savedConfig;
    }

    /**
     * Brez skupne transakcije: izid (tudi neuspeh) se mora zapisati, stranke pa se shranijo v svoji transakciji.
     */
    @Override
    public int syncClients() {
        ClientSyncConfig syncConfig = findSyncConfig();
        if (!syncConfig.isConfigured()) {
            throw new ClientSyncNotConfiguredException();
        }

        try {
            List<SyncedClient> syncedClients =
                    externalClientRepository.fetchClients(syncConfig.apiUrl(), syncConfig.apiToken());
            clientRepository.upsertSyncedClients(syncedClients);
            clientSyncConfigRepository.saveSyncResult(now(), "V redu — " + syncedClients.size() + " strank");
            log.info("Sinhroniziranih {} strank", syncedClients.size());
            return syncedClients.size();
        } catch (ClientSyncFailedException exception) {
            clientSyncConfigRepository.saveSyncResult(now(), "Napaka: " + exception.getReason());
            log.warn("Sinhronizacija strank ni uspela: {}", exception.getReason());
            throw exception;
        } catch (RuntimeException exception) {
            clientSyncConfigRepository.saveSyncResult(now(), "Napaka: shranjevanje strank ni uspelo");
            throw exception;
        }
    }

    private static Instant now() {
        return Instant.now().truncatedTo(ChronoUnit.MILLIS);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

}
