package com.sledenje.teren.vehicle;

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
class VehicleSyncControllerTest {

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
        externalSystem.createContext("/vozila", exchange -> {
            receivedAuthorization = exchange.getRequestHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            byte[] body = externalBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
            exchange.sendResponseHeaders(externalStatus, body.length);
            try (OutputStream responseBody = exchange.getResponseBody()) {
                responseBody.write(body);
            }
        });
        externalSystem.start();
    }

    @AfterAll
    void stopExternalSystem() {
        externalSystem.stop(0);
    }

    @BeforeEach
    void resetState() {
        jdbcTemplate.execute("TRUNCATE TABLE \"vehicles\", \"vehicle_sync_config\"");
        externalStatus = 200;
        externalBody = "[]";
        receivedAuthorization = null;
    }

    private String externalUrl() {
        return "http://127.0.0.1:" + externalSystem.getAddress().getPort() + "/vozila";
    }

    private static MockHttpServletRequestBuilder authorized(MockHttpServletRequestBuilder request) {
        return request.header(HttpHeaders.AUTHORIZATION, AUTHORIZATION_HEADER_VALUE);
    }

    private void saveConnection(String apiUrl, String apiToken) throws Exception {
        mockMvc.perform(authorized(put("/api/v1/vehicles/sync-config"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"apiUrl\": \"" + apiUrl + "\", \"apiToken\": \"" + apiToken + "\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void syncConfigIsEmptyBeforeFirstSave() throws Exception {
        mockMvc.perform(authorized(get("/api/v1/vehicles/sync-config")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.apiUrl").doesNotExist())
                .andExpect(jsonPath("$.hasToken").value(false));
    }

    @Test
    void savingWithoutTokenKeepsExistingTokenAndTokenIsNeverReturned() throws Exception {
        saveConnection(externalUrl(), EXTERNAL_TOKEN);
        saveConnection(externalUrl(), "");

        mockMvc.perform(authorized(get("/api/v1/vehicles/sync-config")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.apiUrl").value(externalUrl()))
                .andExpect(jsonPath("$.hasToken").value(true))
                .andExpect(content().string(not(containsString(EXTERNAL_TOKEN))));
    }

    @Test
    void clearingUrlAlsoClearsToken() throws Exception {
        saveConnection(externalUrl(), EXTERNAL_TOKEN);
        saveConnection("", "");

        mockMvc.perform(authorized(get("/api/v1/vehicles/sync-config")))
                .andExpect(jsonPath("$.apiUrl").doesNotExist())
                .andExpect(jsonPath("$.hasToken").value(false));
    }

    @Test
    void invalidUrlIsRejected() throws Exception {
        mockMvc.perform(authorized(put("/api/v1/vehicles/sync-config"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"apiUrl": "ftp://nekaj"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("Vnesi veljaven URL")));
    }

    @Test
    void syncWithoutConnectionReturnsUnprocessable() throws Exception {
        mockMvc.perform(authorized(post("/api/v1/vehicles/sync")))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.detail").value("Zunanja povezava še ni nastavljena."));
    }

    @Test
    void syncCreatesNewVehiclesAndUpdatesKnownOnes() throws Exception {
        saveConnection(externalUrl(), EXTERNAL_TOKEN);
        externalBody = """
                [{"externalId": "V-1", "plate": " lj 1-abc ", "znamka": "Škoda"},
                 {"externalId": "V-2", "plate": "MB 2-DEF"}]
                """;

        mockMvc.perform(authorized(post("/api/v1/vehicles/sync")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.syncedCount").value(2));
        assertThat(receivedAuthorization).isEqualTo("Bearer " + EXTERNAL_TOKEN);

        externalBody = """
                [{"externalId": "V-1", "plate": "LJ 1-XYZ"}]
                """;
        mockMvc.perform(authorized(post("/api/v1/vehicles/sync")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.syncedCount").value(1));

        List<Map<String, Object>> vehicles = jdbcTemplate.queryForList(
                "SELECT \"externalId\", \"plate\", \"source\"::text AS source FROM \"vehicles\" ORDER BY \"externalId\"");
        assertThat(vehicles).hasSize(2);
        assertThat(vehicles.get(0)).containsEntry("plate", "LJ 1-XYZ").containsEntry("source", "SYNC");
        assertThat(vehicles.get(1)).containsEntry("plate", "MB 2-DEF");

        mockMvc.perform(authorized(get("/api/v1/vehicles/sync-config")))
                .andExpect(jsonPath("$.lastSyncAt").exists())
                .andExpect(jsonPath("$.lastSyncStatus").value("V redu — 1 registrskih št."));
    }

    @Test
    void syncWithPlateOfOtherVehicleSavesNothingAndReturnsConflict() throws Exception {
        jdbcTemplate.update("""
                INSERT INTO "vehicles" ("id", "plate", "source", "updatedAt")
                VALUES ('rocno-vozilo', 'LJ 7-ROC', 'MANUAL', now())
                """);
        saveConnection(externalUrl(), EXTERNAL_TOKEN);
        externalBody = """
                [{"externalId": "V-10", "plate": "KP 10-NOV"}, {"externalId": "V-11", "plate": "lj 7-roc"}]
                """;

        mockMvc.perform(authorized(post("/api/v1/vehicles/sync")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Registrska št. LJ 7-ROC že obstaja."));

        Integer syncedCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM \"vehicles\" WHERE \"source\" = 'SYNC'", Integer.class);
        assertThat(syncedCount).isZero();
        mockMvc.perform(authorized(get("/api/v1/vehicles/sync-config")))
                .andExpect(jsonPath("$.lastSyncStatus").value("Napaka: Registrska št. LJ 7-ROC že obstaja."));
    }

    @Test
    void failedSyncIsRecordedAndReturnsBadGateway() throws Exception {
        saveConnection(externalUrl(), EXTERNAL_TOKEN);
        externalStatus = 503;
        externalBody = "{}";

        mockMvc.perform(authorized(post("/api/v1/vehicles/sync")))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.detail").value("Sinhronizacija ni uspela: HTTP 503"));

        mockMvc.perform(authorized(get("/api/v1/vehicles/sync-config")))
                .andExpect(jsonPath("$.lastSyncStatus").value("Napaka: HTTP 503"));
    }

    @Test
    void syncWithInvalidItemsSavesNothing() throws Exception {
        saveConnection(externalUrl(), EXTERNAL_TOKEN);
        externalBody = """
                [{"externalId": "V-20", "plate": "LJ 20-OK"}, {"externalId": "V-21"}]
                """;

        mockMvc.perform(authorized(post("/api/v1/vehicles/sync")))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.detail", startsWith("Sinhronizacija ni uspela")));

        Integer savedCount = jdbcTemplate.queryForObject("SELECT count(*) FROM \"vehicles\"", Integer.class);
        assertThat(savedCount).isZero();
    }

}
