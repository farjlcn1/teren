package com.sledenje.teren.common.infrastructure.external;

import java.net.URI;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Prenos JSON seznama z zunanjega sistema (GET, neobvezen Bearer žeton). Skupno sinhronizacijam strank in vozil.
 */
@Component
public class ExternalListFetcher {

    private static final Logger log = LoggerFactory.getLogger(ExternalListFetcher.class);

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(60);

    private final RestClient restClient;

    public ExternalListFetcher() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(CONNECT_TIMEOUT);
        requestFactory.setReadTimeout(READ_TIMEOUT);
        this.restClient = RestClient.builder().requestFactory(requestFactory).build();
    }

    /**
     * @param apiToken      Bearer žeton ali {@code null}, če ga zunanji sistem ne zahteva
     * @param itemArrayType tip elementov kot tabela, npr. {@code ExternalClient[].class}
     * @return elementi seznama; posamezen element je lahko {@code null}, preveri ga klicatelj
     * @throws ExternalFetchException če sistem ni dosegljiv, vrne napako ali odgovor ni JSON seznam
     */
    public <T> List<T> fetchList(String apiUrl, String apiToken, Class<T[]> itemArrayType) {
        T[] items;
        try {
            items = restClient.get()
                    .uri(URI.create(apiUrl))
                    .headers(headers -> {
                        if (apiToken != null) {
                            headers.setBearerAuth(apiToken);
                        }
                    })
                    .retrieve()
                    .body(itemArrayType);
        } catch (RestClientResponseException exception) {
            throw new ExternalFetchException("HTTP " + exception.getStatusCode().value());
        } catch (RestClientException | IllegalArgumentException exception) {
            log.warn("Klic zunanjega sistema ni uspel: {}", apiUrl, exception);
            throw new ExternalFetchException("zunanja povezava ni dosegljiva ali je vrnila neveljaven odgovor");
        }
        if (items == null) {
            throw new ExternalFetchException("zunanja povezava je vrnila prazen odgovor");
        }
        return Arrays.asList(items);
    }

}
