package com.sledenje.teren.client.infrastructure.api.repository;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sledenje.teren.client.domain.exception.ClientSyncFailedException;
import com.sledenje.teren.client.domain.model.SyncedClient;
import com.sledenje.teren.client.domain.repository.ExternalClientRepository;
import com.sledenje.teren.common.infrastructure.external.ExternalFetchException;
import com.sledenje.teren.common.infrastructure.external.ExternalListFetcher;
import java.util.List;
import org.springframework.stereotype.Repository;

/**
 * Generična povezava: GET na nastavljeni URL vrne JSON seznam objektov s polji
 * {@code externalId}, {@code name}, {@code address}, {@code contactInfo}.
 */
@Repository
public class ExternalClientRepositoryImpl implements ExternalClientRepository {

    private static final String INVALID_RESPONSE = "zunanja povezava je vrnila neveljaven seznam strank";

    private final ExternalListFetcher externalListFetcher;

    public ExternalClientRepositoryImpl(ExternalListFetcher externalListFetcher) {
        this.externalListFetcher = externalListFetcher;
    }

    @Override
    public List<SyncedClient> fetchClients(String apiUrl, String apiToken) {
        List<ExternalClient> externalClients;
        try {
            externalClients = externalListFetcher.fetchList(apiUrl, apiToken, ExternalClient[].class);
        } catch (ExternalFetchException exception) {
            throw new ClientSyncFailedException(exception.getReason());
        }
        return externalClients.stream().map(ExternalClientRepositoryImpl::toSyncedClient).toList();
    }

    private static SyncedClient toSyncedClient(ExternalClient externalClient) {
        if (externalClient == null || isBlank(externalClient.externalId()) || isBlank(externalClient.name())) {
            throw new ClientSyncFailedException(INVALID_RESPONSE);
        }
        return new SyncedClient(
                externalClient.externalId().trim(),
                externalClient.name().trim(),
                blankToNull(externalClient.address()),
                blankToNull(externalClient.contactInfo()));
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String blankToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ExternalClient(String externalId, String name, String address, String contactInfo) {
    }

}
