package com.sledenje.teren.client;

import static com.sledenje.teren.TestcontainersConfiguration.AUTHORIZATION_HEADER_VALUE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sledenje.teren.TestcontainersConfiguration;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ClientSyncControllerTest {

    private static final String EXTERNAL_TOKEN = "zunanji-zeton";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Lažni zunanji sistem: vrne {@link #externalStatus} in {@link #externalBody}, zapomni si glavo Authorization.
     */
    private HttpServer externalSystem;
    private volatile int externalStatus;
    private volatile String externalBody;
    private volatile String receivedAuthorization;

    @BeforeAll
    void startExternalSystem() throws IOException {
        externalSystem = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        externalSystem.createContext("/stranke", exchange -> {
            receivedAuthorization = exchange.getRequestHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            byte[] body = externalBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
            exchange.sendResponseHeaders(externalStatus, body.length);
            try (OutputStream responseBody = exchange.getResponseBody()) {
                responseBody.write(body);
            }
        });
        externalSystem.createContext("/preusmeritev", exchange -> {
            exchange.getResponseHeaders().add(HttpHeaders.LOCATION, "/stranke");
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        });
        externalSystem.start();
    }

    @AfterAll
    void stopExternalSystem() {
        externalSystem.stop(0);
    }

    @BeforeEach
    void resetState() {
        jdbcTemplate.execute("TRUNCATE TABLE \"clients\", \"client_sync_config\" CASCADE");
        externalStatus = 200;
        externalBody = "[]";
        receivedAuthorization = null;
    }

    private String externalUrl() {
        return "http://127.0.0.1:" + externalSystem.getAddress().getPort() + "/stranke";
    }

    private static MockHttpServletRequestBuilder authorized(MockHttpServletRequestBuilder request) {
        return request.header(HttpHeaders.AUTHORIZATION, AUTHORIZATION_HEADER_VALUE);
    }

    private void saveConnection(String apiUrl, String apiToken) throws Exception {
        mockMvc.perform(authorized(put("/api/v1/clients/sync-config"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"apiUrl\": \"" + apiUrl + "\", \"apiToken\": \"" + apiToken + "\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void syncConfigIsEmptyBeforeFirstSave() throws Exception {
        mockMvc.perform(authorized(get("/api/v1/clients/sync-config")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.apiUrl").doesNotExist())
                .andExpect(jsonPath("$.hasToken").value(false));
    }

    @Test
    void savingWithoutTokenKeepsExistingTokenAndTokenIsNeverReturned() throws Exception {
        saveConnection(externalUrl(), EXTERNAL_TOKEN);
        saveConnection(externalUrl(), "");

        mockMvc.perform(authorized(get("/api/v1/clients/sync-config")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.apiUrl").value(externalUrl()))
                .andExpect(jsonPath("$.hasToken").value(true))
                .andExpect(content().string(not(containsString(EXTERNAL_TOKEN))));
    }

    @Test
    void clearingUrlAlsoClearsToken() throws Exception {
        saveConnection(externalUrl(), EXTERNAL_TOKEN);
        saveConnection("", "");

        mockMvc.perform(authorized(get("/api/v1/clients/sync-config")))
                .andExpect(jsonPath("$.apiUrl").doesNotExist())
                .andExpect(jsonPath("$.hasToken").value(false));
    }

    @Test
    void invalidUrlIsRejected() throws Exception {
        mockMvc.perform(authorized(put("/api/v1/clients/sync-config"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"apiUrl": "ftp://nekaj"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("Vnesi veljaven URL")));
    }

    @Test
    void syncWithoutConnectionReturnsUnprocessable() throws Exception {
        mockMvc.perform(authorized(post("/api/v1/clients/sync")))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.detail").value("Zunanja povezava še ni nastavljena."));
    }

    @Test
    void syncCreatesNewClientsAndUpdatesKnownOnes() throws Exception {
        saveConnection(externalUrl(), EXTERNAL_TOKEN);
        externalBody = """
                [{"externalId": "A-1", "name": "Prva", "address": "Naslov 1", "dodatno": true},
                 {"externalId": "A-2", "name": "Druga"}]
                """;

        mockMvc.perform(authorized(post("/api/v1/clients/sync")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.syncedCount").value(2));
        assertThat(receivedAuthorization).isEqualTo("Bearer " + EXTERNAL_TOKEN);

        externalBody = """
                [{"externalId": "A-1", "name": "Prva preimenovana"}]
                """;
        mockMvc.perform(authorized(post("/api/v1/clients/sync")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.syncedCount").value(1));

        List<Map<String, Object>> clients = jdbcTemplate.queryForList(
                "SELECT \"externalId\", \"name\", \"address\", \"source\"::text AS source FROM \"clients\" ORDER BY \"externalId\"");
        assertThat(clients).hasSize(2);
        assertThat(clients.get(0)).containsEntry("name", "Prva preimenovana").containsEntry("source", "SYNC");
        assertThat(clients.get(0).get("address")).isNull();
        assertThat(clients.get(1)).containsEntry("name", "Druga");

        mockMvc.perform(authorized(get("/api/v1/clients/sync-config")))
                .andExpect(jsonPath("$.lastSyncAt").exists())
                .andExpect(jsonPath("$.lastSyncStatus").value("V redu — 1 strank"));
    }

    @Test
    void failedSyncIsRecordedAndReturnsBadGateway() throws Exception {
        saveConnection(externalUrl(), EXTERNAL_TOKEN);
        externalStatus = 500;
        externalBody = "{\"error\": \"interna napaka\"}";

        mockMvc.perform(authorized(post("/api/v1/clients/sync")))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.detail").value("Sinhronizacija ni uspela: HTTP 500"));

        mockMvc.perform(authorized(get("/api/v1/clients/sync-config")))
                .andExpect(jsonPath("$.lastSyncStatus").value("Napaka: HTTP 500"));
    }

    @Test
    void changingUrlWithoutTokenDropsOldToken() throws Exception {
        saveConnection(externalUrl(), EXTERNAL_TOKEN);
        saveConnection(externalUrl() + "?drug=1", "");

        mockMvc.perform(authorized(get("/api/v1/clients/sync-config")))
                .andExpect(jsonPath("$.hasToken").value(false));
    }

    @Test
    void redirectsAreNotFollowed() throws Exception {
        saveConnection(externalUrl().replace("/stranke", "/preusmeritev"), EXTERNAL_TOKEN);
        externalBody = "[{\"externalId\": \"R-1\", \"name\": \"Preusmerjena\"}]";

        mockMvc.perform(authorized(post("/api/v1/clients/sync")))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.detail").value("Sinhronizacija ni uspela: preusmeritve niso dovoljene"));
        assertThat(receivedAuthorization).isNull();
    }

    @Test
    void syncWithInvalidItemsSavesNothing() throws Exception {
        saveConnection(externalUrl(), EXTERNAL_TOKEN);
        externalBody = """
                [{"externalId": "B-1", "name": "Veljavna"}, {"externalId": "B-2"}]
                """;

        mockMvc.perform(authorized(post("/api/v1/clients/sync")))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.detail", startsWith("Sinhronizacija ni uspela")));

        Integer savedCount = jdbcTemplate.queryForObject("SELECT count(*) FROM \"clients\"", Integer.class);
        assertThat(savedCount).isZero();
    }

}
