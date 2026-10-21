package com.sledenje.teren.vehicle;

import static com.sledenje.teren.TestcontainersConfiguration.AUTHORIZATION_HEADER_VALUE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.sledenje.teren.TestcontainersConfiguration;
import java.util.List;
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
class VehicleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanVehicles() {
        jdbcTemplate.execute("TRUNCATE TABLE \"vehicles\"");
    }

    private static MockHttpServletRequestBuilder authorized(MockHttpServletRequestBuilder request) {
        return request.header(HttpHeaders.AUTHORIZATION, AUTHORIZATION_HEADER_VALUE);
    }

    private String createVehicle(String plate) throws Exception {
        String responseBody = mockMvc.perform(authorized(post("/api/v1/vehicles"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"plate\": \"" + plate + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(responseBody, "$.id");
    }

    @Test
    void requestWithoutTokenIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/vehicles"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createVehicleNormalizesPlate() throws Exception {
        mockMvc.perform(authorized(post("/api/v1/vehicles"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"plate": "  lj 12-abč "}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/v1/vehicles/")))
                .andExpect(jsonPath("$.plate").value("LJ 12-ABČ"))
                .andExpect(jsonPath("$.source").value("MANUAL"));
    }

    @Test
    void createDuplicatePlateReturnsConflict() throws Exception {
        createVehicle("LJ 12-ABC");

        mockMvc.perform(authorized(post("/api/v1/vehicles"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"plate": "lj 12-abc"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Registrska št. LJ 12-ABC že obstaja."));
    }

    @Test
    void createWithoutPlateReturnsBadRequest() throws Exception {
        mockMvc.perform(authorized(post("/api/v1/vehicles"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"plate": " "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("Registrska št. je obvezna")));
    }

    @Test
    void listIsOrderedByPlate() throws Exception {
        createVehicle("NM 99-ZZZ");
        createVehicle("CE 11-AAA");

        mockMvc.perform(authorized(get("/api/v1/vehicles")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].plate").value("CE 11-AAA"))
                .andExpect(jsonPath("$[1].plate").value("NM 99-ZZZ"));
    }

    @Test
    void updatePlateToOwnPlateIsAllowedButToOtherVehiclePlateIsConflict() throws Exception {
        String vehicleId = createVehicle("KR 10-AAA");
        createVehicle("KR 20-BBB");

        mockMvc.perform(authorized(put("/api/v1/vehicles/" + vehicleId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"plate": "kr 10-aaa"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plate").value("KR 10-AAA"));

        mockMvc.perform(authorized(put("/api/v1/vehicles/" + vehicleId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"plate": "KR 20-BBB"}
                                """))
                .andExpect(status().isConflict());

        mockMvc.perform(authorized(put("/api/v1/vehicles/" + vehicleId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"plate": "KR 30-CCC"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plate").value("KR 30-CCC"));
    }

    @Test
    void updateUnknownVehicleReturnsNotFound() throws Exception {
        mockMvc.perform(authorized(put("/api/v1/vehicles/ne-obstaja"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"plate": "LJ 1-A"}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteVehicleRemovesIt() throws Exception {
        String vehicleId = createVehicle("PO 55-XYZ");

        mockMvc.perform(authorized(delete("/api/v1/vehicles/" + vehicleId)))
                .andExpect(status().isNoContent());
        mockMvc.perform(authorized(get("/api/v1/vehicles/" + vehicleId)))
                .andExpect(status().isNotFound());
        mockMvc.perform(authorized(delete("/api/v1/vehicles/" + vehicleId)))
                .andExpect(status().isNotFound());
    }

    @Test
    void importSkipsExistingAndRepeatedPlates() throws Exception {
        createVehicle("LJ 1-OBST");

        mockMvc.perform(authorized(post("/api/v1/vehicles/import"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"plates": ["lj 1-obst", "MB 2-NOVA", "mb 2-nova", "GO 3-NOVA"]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.createdCount").value(2))
                .andExpect(jsonPath("$.skippedCount").value(2));

        List<String> importedPlates = jdbcTemplate.queryForList(
                "SELECT \"plate\" FROM \"vehicles\" WHERE \"source\" = 'IMPORT' ORDER BY \"plate\"", String.class);
        assertThat(importedPlates).containsExactly("GO 3-NOVA", "MB 2-NOVA");
    }

    @Test
    void readsVehicleWrittenByPrisma() throws Exception {
        // Zapis v obliki, kot ga shrani Prisma: cuid ID, enum "ClientSource", čas v UTC brez časovnega pasu.
        jdbcTemplate.update("""
                INSERT INTO "vehicles" ("id", "plate", "source", "createdAt", "updatedAt")
                VALUES ('cmd8x2k0q0000vozilo00001', 'LJ PRISMA-1', 'SYNC',
                        '2026-07-24 07:32:07.123', '2026-07-24 07:32:07.123')
                """);

        mockMvc.perform(authorized(get("/api/v1/vehicles/cmd8x2k0q0000vozilo00001")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plate").value("LJ PRISMA-1"))
                .andExpect(jsonPath("$.source").value("SYNC"))
                .andExpect(jsonPath("$.createdAt").value("2026-07-24T07:32:07.123Z"));
    }

}
