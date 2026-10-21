package com.sledenje.teren.client.infrastructure.api.repository;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sledenje.teren.client.domain.exception.ClientSyncFailedException;
import com.sledenje.teren.client.domain.model.SyncedClient;
import com.sledenje.teren.client.domain.repository.ExternalClientRepository;
import java.net.URI;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Generična povezava: GET na nastavljeni URL vrne JSON seznam objektov s polji
 * {@code externalId}, {@code name}, {@code address}, {@code contactInfo}.
 */
@Repository
public class ExternalClientRepositoryImpl implements ExternalClientRepository {

    private static final Logger log = LoggerFactory.getLogger(ExternalClientRepositoryImpl.class);

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(60);

    private static final String INVALID_RESPONSE = "zunanja povezava je vrnila neveljaven seznam strank";

    private final RestClient restClient;

    public ExternalClientRepositoryImpl() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(CONNECT_TIMEOUT);
        requestFactory.setReadTimeout(READ_TIMEOUT);
        this.restClient = RestClient.builder().requestFactory(requestFactory).build();
    }

    @Override
    public List<SyncedClient> fetchClients(String apiUrl, String apiToken) {
        ExternalClient[] externalClients;
        try {
            externalClients = restClient.get()
                    .uri(URI.create(apiUrl))
                    .headers(headers -> {
                        if (apiToken != null) {
                            headers.setBearerAuth(apiToken);
                        }
                    })
                    .retrieve()
                    .body(ExternalClient[].class);
        } catch (RestClientResponseException exception) {
            throw new ClientSyncFailedException("HTTP " + exception.getStatusCode().value());
        } catch (RestClientException | IllegalArgumentException exception) {
            log.warn("Klic zunanje povezave za stranke ni uspel: {}", apiUrl, exception);
            throw new ClientSyncFailedException("zunanja povezava ni dosegljiva ali je vrnila neveljaven odgovor");
        }

        if (externalClients == null) {
            throw new ClientSyncFailedException(INVALID_RESPONSE);
        }
        return Arrays.stream(externalClients).map(ExternalClientRepositoryImpl::toSyncedClient).toList();
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
