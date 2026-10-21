package com.sledenje.teren.common.infrastructure.external;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Prenos JSON seznama z zunanjega sistema (GET, neobvezen Bearer žeton). Skupno sinhronizacijam strank in vozil.
 * <p>
 * URL vnese administrator, zato API ne sme postati most v notranje omrežje (SSRF): zavrne lokalne naslove
 * (loopback, link-local z metadata naslovom 169.254.169.254, multicast), imena brez pike (Docker servisi
 * {@code db}, {@code api}, {@code teren}, {@code localhost}) in ne sledi preusmeritvam. Naslovi v lokalnem
 * omrežju (npr. 192.168.x.x) so dovoljeni, ker je tam lahko zunanji sistem podjetja.
 */
@Component
public class ExternalListFetcher {

    private static final Logger log = LoggerFactory.getLogger(ExternalListFetcher.class);

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(60);

    private final RestClient restClient;
    private final boolean allowLocalAddresses;

    /**
     * @param allowLocalAddresses samo za teste z lažnim strežnikom na 127.0.0.1
     */
    public ExternalListFetcher(@Value("${teren.api.sync.allow-local-addresses:false}") boolean allowLocalAddresses) {
        this.allowLocalAddresses = allowLocalAddresses;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory() {
            @Override
            protected void prepareConnection(HttpURLConnection connection, String httpMethod) throws IOException {
                super.prepareConnection(connection, httpMethod);
                // Preusmeritev bi lahko vodila na notranji naslov, ki ga requireAllowedTarget ne bi preveril.
                connection.setInstanceFollowRedirects(false);
            }
        };
        requestFactory.setConnectTimeout(CONNECT_TIMEOUT);
        requestFactory.setReadTimeout(READ_TIMEOUT);
        this.restClient = RestClient.builder().requestFactory(requestFactory).build();
    }

    /**
     * @param apiToken      Bearer žeton ali {@code null}, če ga zunanji sistem ne zahteva
     * @param itemArrayType tip elementov kot tabela, npr. {@code ExternalClient[].class}
     * @return elementi seznama; posamezen element je lahko {@code null}, preveri ga klicatelj
     * @throws ExternalFetchException če naslov ni dovoljen, sistem ni dosegljiv, vrne napako ali odgovor ni JSON seznam
     */
    public <T> List<T> fetchList(String apiUrl, String apiToken, Class<T[]> itemArrayType) {
        URI uri = requireAllowedTarget(apiUrl);
        T[] items;
        try {
            items = restClient.get()
                    .uri(uri)
                    .headers(headers -> {
                        if (apiToken != null) {
                            headers.setBearerAuth(apiToken);
                        }
                    })
                    .retrieve()
                    .onStatus(HttpStatusCode::is3xxRedirection, (request, response) -> {
                        throw new ExternalFetchException("preusmeritve niso dovoljene");
                    })
                    .body(itemArrayType);
        } catch (RestClientResponseException exception) {
            throw new ExternalFetchException("HTTP " + exception.getStatusCode().value());
        } catch (RestClientException exception) {
            log.warn("Klic zunanjega sistema ni uspel: {}", apiUrl, exception);
            throw new ExternalFetchException("zunanja povezava ni dosegljiva ali je vrnila neveljaven odgovor");
        }
        if (items == null) {
            throw new ExternalFetchException("zunanja povezava je vrnila prazen odgovor");
        }
        return Arrays.asList(items);
    }

    URI requireAllowedTarget(String apiUrl) {
        URI uri;
        try {
            uri = URI.create(apiUrl);
        } catch (IllegalArgumentException exception) {
            throw new ExternalFetchException("neveljaven URL");
        }
        String host = uri.getHost();
        if (host == null || !("http".equals(uri.getScheme()) || "https".equals(uri.getScheme()))) {
            throw new ExternalFetchException("neveljaven URL");
        }
        if (allowLocalAddresses) {
            return uri;
        }
        if (!host.contains(".") && !host.contains(":")) {
            throw new ExternalFetchException("notranji naslov ni dovoljen");
        }
        try {
            for (InetAddress address : InetAddress.getAllByName(host)) {
                if (address.isLoopbackAddress() || address.isLinkLocalAddress() || address.isAnyLocalAddress()
                        || address.isMulticastAddress()) {
                    throw new ExternalFetchException("notranji naslov ni dovoljen");
                }
            }
        } catch (UnknownHostException exception) {
            throw new ExternalFetchException("zunanja povezava ni dosegljiva ali je vrnila neveljaven odgovor");
        }
        return uri;
    }

}
