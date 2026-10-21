package com.sledenje.teren.common.infrastructure.external;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ExternalListFetcherTest {

    private final ExternalListFetcher fetcher = new ExternalListFetcher(false);

    @ParameterizedTest
    @ValueSource(strings = {
            "http://127.0.0.1:8080/api/v1/users",
            "http://localhost/",
            "http://db:5432/",
            "http://api:8080/api/v1/users",
            "http://169.254.169.254/latest/meta-data/",
            "http://[::1]/",
            "http://0.0.0.0/",
            "http://127.1.2.3.nip.io/"})
    void internalTargetsAreRejected(String url) {
        assertThatThrownBy(() -> fetcher.requireAllowedTarget(url))
                .isInstanceOf(ExternalFetchException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ftp://crm.example.com/", "file:///etc/passwd", "ni url"})
    void nonHttpUrlsAreRejected(String url) {
        assertThatThrownBy(() -> fetcher.requireAllowedTarget(url))
                .isInstanceOf(ExternalFetchException.class)
                .hasMessage("neveljaven URL");
    }

    @ParameterizedTest
    @ValueSource(strings = {"http://192.168.0.30:8080/stranke", "https://8.8.8.8/stranke"})
    void lanAndPublicAddressesAreAllowed(String url) {
        assertThat(fetcher.requireAllowedTarget(url).toString()).isEqualTo(url);
    }

}
