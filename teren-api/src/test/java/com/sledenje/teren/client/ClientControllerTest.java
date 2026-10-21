package com.sledenje.teren.client;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sledenje.teren.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

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
        jdbcTemplate.execute("TRUNCATE TABLE \"clients\" CASCADE");
    }

    @Test
    void createClientReturnsCreatedWithLocation() throws Exception {
        mockMvc.perform(post("/api/v1/clients")
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
        mockMvc.perform(post("/api/v1/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": ""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("Ime je obvezno")));
    }

    @Test
    void unknownClientReturnsNotFoundWithoutInternalDetails() throws Exception {
        mockMvc.perform(get("/api/v1/clients/ne-obstaja"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value(containsString("ne obstaja")))
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(content().string(not(containsString("Exception"))));
    }

}
