package com.sledenje.teren.user;

import static com.sledenje.teren.TestcontainersConfiguration.AUTHORIZATION_HEADER_VALUE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.sledenje.teren.TestcontainersConfiguration;
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
class UserControllerTest {

    static final String ALL_PERMISSIONS_OFF = """
            {"isActive": true, "canManageUsers": false, "canViewAllOrders": false, "canExportData": false,
             "canSendEmail": false, "canEditOrders": false, "canManageClients": false,
             "canManageVehicles": false, "canManagePlan": false}
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanUsers() {
        jdbcTemplate.execute("TRUNCATE TABLE \"users\", \"clients\" CASCADE");
    }

    private static MockHttpServletRequestBuilder authorized(MockHttpServletRequestBuilder request) {
        return request.header(HttpHeaders.AUTHORIZATION, AUTHORIZATION_HEADER_VALUE);
    }

    private String createUser(String email, String password) throws Exception {
        String responseBody = mockMvc.perform(authorized(post("/api/v1/users"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"" + email + "\", \"fullName\": \"Test Uporabnik\", \"password\": \""
                                + password + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(responseBody, "$.id");
    }

    @Test
    void createdUserIsActiveWithoutPermissionsAndWithoutPasswordInResponse() throws Exception {
        mockMvc.perform(authorized(post("/api/v1/users"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "Janez.Novak@Example.com", "fullName": " Janez Novak ", "password": "Varno-geslo1"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("janez.novak@example.com"))
                .andExpect(jsonPath("$.fullName").value("Janez Novak"))
                .andExpect(jsonPath("$.isActive").value(true))
                .andExpect(jsonPath("$.canManageUsers").value(false))
                .andExpect(jsonPath("$.canManagePlan").value(false))
                .andExpect(content().string(not(containsString("password"))))
                .andExpect(content().string(not(containsString("Varno-geslo1"))));

        String storedHash = jdbcTemplate.queryForObject(
                "SELECT \"passwordHash\" FROM \"users\" WHERE \"email\" = 'janez.novak@example.com'", String.class);
        assertThat(storedHash).startsWith("$2a$12$");
    }

    @Test
    void duplicateEmailIsRejectedRegardlessOfCase() throws Exception {
        createUser("ana@example.com", "Varno-geslo1");

        mockMvc.perform(authorized(post("/api/v1/users"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "ANA@example.com", "fullName": "Ana", "password": "Varno-geslo1"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Uporabnik s tem emailom že obstaja."));
    }

    @Test
    void weakPasswordIsRejectedWithSameMessagesAsApp() throws Exception {
        String[][] cases = {
                {"Kratk1!", "Geslo mora imeti vsaj 8 znakov"},
                {"brez-velike1", "Geslo mora vsebovati vsaj eno veliko črko"},
                {"Brez-stevilke", "Geslo mora vsebovati vsaj eno številko"},
                {"BrezPosebnega1", "Geslo mora vsebovati vsaj en poseben znak"},
        };
        for (String[] passwordAndMessage : cases) {
            mockMvc.perform(authorized(post("/api/v1/users"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\": \"sibko@example.com\", \"fullName\": \"Šibko\", \"password\": \""
                                    + passwordAndMessage[0] + "\"}"))
                    .andExpect(status().is(422))
                    .andExpect(jsonPath("$.detail").value(passwordAndMessage[1]));
        }
        // "č" šteje za poseben znak, tako kot v aplikaciji.
        createUser("cevapcici@example.com", "Cevapcici1č");
    }

    @Test
    void invalidEmailIsRejected() throws Exception {
        mockMvc.perform(authorized(post("/api/v1/users"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "ni-email", "fullName": "X", "password": "Varno-geslo1"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("Vnesi veljaven email.")));
    }

    @Test
    void updateAccessSetsAllPermissionsIncludingPlan() throws Exception {
        String userId = createUser("planer@example.com", "Varno-geslo1");

        mockMvc.perform(authorized(put("/api/v1/users/" + userId + "/access"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ALL_PERMISSIONS_OFF.replace("\"canManagePlan\": false", "\"canManagePlan\": true")
                                .replace("\"isActive\": true", "\"isActive\": false")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isActive").value(false))
                .andExpect(jsonPath("$.canManagePlan").value(true))
                .andExpect(jsonPath("$.canManageUsers").value(false));

        mockMvc.perform(authorized(get("/api/v1/users/" + userId)))
                .andExpect(jsonPath("$.canManagePlan").value(true));
    }

    @Test
    void updateAccessWithMissingPermissionIsRejected() throws Exception {
        String userId = createUser("delno@example.com", "Varno-geslo1");

        mockMvc.perform(authorized(put("/api/v1/users/" + userId + "/access"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"isActive": true, "canManageUsers": true}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void changePasswordValidatesPolicyAndUnknownUser() throws Exception {
        String userId = createUser("geslo@example.com", "Varno-geslo1");

        mockMvc.perform(authorized(put("/api/v1/users/" + userId + "/password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"password": "slabo"}
                                """))
                .andExpect(status().is(422));
        mockMvc.perform(authorized(put("/api/v1/users/" + userId + "/password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"password": "Novo-geslo2"}
                                """))
                .andExpect(status().isNoContent());
        mockMvc.perform(authorized(put("/api/v1/users/ne-obstaja/password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"password": "Novo-geslo2"}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteUserWithoutHistoryRemovesIt() throws Exception {
        String userId = createUser("brisanje@example.com", "Varno-geslo1");

        mockMvc.perform(authorized(delete("/api/v1/users/" + userId)))
                .andExpect(status().isNoContent());
        mockMvc.perform(authorized(get("/api/v1/users/" + userId)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteUserWithPlanGroupReturnsConflict() throws Exception {
        String userId = createUser("zgodovina@example.com", "Varno-geslo1");
        jdbcTemplate.update("""
                INSERT INTO "clients" ("id", "name", "source", "updatedAt") VALUES ('stranka-plan', 'Stranka', 'MANUAL', now())
                """);
        jdbcTemplate.update("""
                INSERT INTO "plan_groups" ("id", "clientId", "startAt", "endAt", "createdById", "updatedAt")
                VALUES ('dogodek-uporabnika', 'stranka-plan', now(), now() + interval '1 hour', ?, now())
                """, userId);

        mockMvc.perform(authorized(delete("/api/v1/users/" + userId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(containsString("Namesto tega ga onemogoči")));
    }

    @Test
    void listIsInCreationOrder() throws Exception {
        createUser("prvi@example.com", "Varno-geslo1");
        createUser("drugi@example.com", "Varno-geslo1");

        mockMvc.perform(authorized(get("/api/v1/users")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("prvi@example.com"))
                .andExpect(jsonPath("$[1].email").value("drugi@example.com"));
    }

}
