package com.sledenje.teren.client;

import static com.sledenje.teren.TestcontainersConfiguration.AUTHORIZATION_HEADER_VALUE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.sledenje.teren.TestcontainersConfiguration;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
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
class ClientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeAll
    void prepareDatabase() {
        jdbcTemplate.execute("TRUNCATE TABLE \"clients\", \"users\" CASCADE");
    }

    private static MockHttpServletRequestBuilder authorized(MockHttpServletRequestBuilder request) {
        return request.header(HttpHeaders.AUTHORIZATION, AUTHORIZATION_HEADER_VALUE);
    }

    private String createClient(String name) throws Exception {
        String responseBody = mockMvc.perform(authorized(post("/api/v1/clients"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"" + name + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(responseBody, "$.id");
    }

    @Test
    void requestWithoutTokenIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/clients"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
                .andExpect(jsonPath("$.detail").value(containsString("API žeton")));
    }

    @Test
    void requestWithWrongTokenIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/clients").header(HttpHeaders.AUTHORIZATION, "Bearer napacen-zeton"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void pathTricksDoNotBypassToken() throws Exception {
        for (String path : List.of("/api;x/v1/clients", "/%61pi/v1/clients", "/api/v1/clients;x",
                "/actuator/health/../../api/v1/clients", "//api/v1/clients", "/v3/api-docs/../../api/v1/clients")) {
            mockMvc.perform(get(URI.create(path)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Test
    void apiDocsDoNotRequireToken() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk());
    }

    @Test
    void healthDoesNotRequireToken() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void createClientReturnsCreatedWithLocation() throws Exception {
        mockMvc.perform(authorized(post("/api/v1/clients"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Prevozi Novak d.o.o.", "address": "  ", "contactInfo": "041 123 456"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/v1/clients/")))
                .andExpect(jsonPath("$.name").value("Prevozi Novak d.o.o."))
                .andExpect(jsonPath("$.address").doesNotExist())
                .andExpect(jsonPath("$.source").value("MANUAL"));
    }

    @Test
    void createClientWithoutNameReturnsBadRequest() throws Exception {
        mockMvc.perform(authorized(post("/api/v1/clients"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": ""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("Ime je obvezno")));
    }

    @Test
    void unknownClientReturnsNotFoundWithoutInternalDetails() throws Exception {
        mockMvc.perform(authorized(get("/api/v1/clients/ne-obstaja")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value(containsString("ne obstaja")))
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(content().string(not(containsString("Exception"))));
    }

    @Test
    void updateClientReplacesDetailsAndClearsBlankFields() throws Exception {
        String clientId = createClient("Staro ime");
        mockMvc.perform(authorized(put("/api/v1/clients/" + clientId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": " Novo ime ", "address": "Celovška 5", "contactInfo": ""}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Novo ime"))
                .andExpect(jsonPath("$.address").value("Celovška 5"))
                .andExpect(jsonPath("$.contactInfo").doesNotExist())
                .andExpect(jsonPath("$.source").value("MANUAL"));

        mockMvc.perform(authorized(put("/api/v1/clients/" + clientId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Novo ime", "address": null}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.address").doesNotExist());
    }

    @Test
    void updateUnknownClientReturnsNotFound() throws Exception {
        mockMvc.perform(authorized(put("/api/v1/clients/ne-obstaja"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Ime"}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteClientRemovesIt() throws Exception {
        String clientId = createClient("Za brisanje");

        mockMvc.perform(authorized(delete("/api/v1/clients/" + clientId)))
                .andExpect(status().isNoContent());
        mockMvc.perform(authorized(get("/api/v1/clients/" + clientId)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteClientWithPlanGroupReturnsConflict() throws Exception {
        String clientId = createClient("Stranka s planom");
        jdbcTemplate.update("""
                INSERT INTO "users" ("id", "email", "passwordHash", "fullName", "updatedAt")
                VALUES ('uporabnik-plan', 'plan@example.com', 'x', 'Planer', now())
                """);
        jdbcTemplate.update("""
                INSERT INTO "plan_groups" ("id", "clientId", "startAt", "endAt", "createdById", "updatedAt")
                VALUES ('dogodek-1', ?, now(), now() + interval '1 hour', 'uporabnik-plan', now())
                """, clientId);

        mockMvc.perform(authorized(delete("/api/v1/clients/" + clientId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(containsString("ni mogoče izbrisati")));
        mockMvc.perform(authorized(get("/api/v1/clients/" + clientId)))
                .andExpect(status().isOk());
    }

    @Test
    void importClientsCreatesAllWithImportSource() throws Exception {
        mockMvc.perform(authorized(post("/api/v1/clients/import"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"clients": [
                                  {"name": "Uvoz Ena", "address": "Naslov 1"},
                                  {"name": "Uvoz Dva", "contactInfo": "040 000 000"}
                                ]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.createdCount").value(2));

        Integer importedCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM \"clients\" WHERE \"source\" = 'IMPORT' AND \"name\" LIKE 'Uvoz %'",
                Integer.class);
        assertThat(importedCount).isEqualTo(2);
    }

    @Test
    void importWithClientWithoutNameSavesNothing() throws Exception {
        mockMvc.perform(authorized(post("/api/v1/clients/import"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"clients": [{"name": "Neveljaven uvoz"}, {"name": " "}]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("clients[1].name")));

        Integer savedCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM \"clients\" WHERE \"name\" = 'Neveljaven uvoz'", Integer.class);
        assertThat(savedCount).isZero();
    }

}
