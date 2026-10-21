package com.sledenje.teren.user;

import static com.sledenje.teren.TestcontainersConfiguration.AUTHORIZATION_HEADER_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AuthenticationControllerTest {

    /**
     * Hash gesla "Staro-geslo1", kot ga je ustvarila aplikacija Teren (bcryptjs, $2b$, cena 4 za hitrejše teste).
     */
    private static final String BCRYPTJS_HASH = "$2b$04$39F3BtIudCd8bnCvLpu9FuhCSHHUP4PBMqUAEn5mcvkautcXfyp8C";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void prepareUsers() {
        jdbcTemplate.execute("TRUNCATE TABLE \"users\" CASCADE");
        jdbcTemplate.update("""
                INSERT INTO "users" ("id", "email", "passwordHash", "fullName", "isActive", "canManagePlan", "updatedAt")
                VALUES ('prisma-uporabnik', 'stari@example.com', ?, 'Stari Uporabnik', true, true, now()),
                       ('onemogocen', 'onemogocen@example.com', ?, 'Onemogočen', false, false, now())
                """, BCRYPTJS_HASH, BCRYPTJS_HASH);
    }

    private static MockHttpServletRequestBuilder authorized(MockHttpServletRequestBuilder request) {
        return request.header(HttpHeaders.AUTHORIZATION, AUTHORIZATION_HEADER_VALUE);
    }

    private ResultActions login(String email, String password) throws Exception {
        return mockMvc.perform(authorized(post("/api/v1/auth/login"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\": \"" + email + "\", \"password\": \"" + password + "\"}"));
    }

    @Test
    void userWithPasswordHashedByAppCanLogIn() throws Exception {
        login(" Stari@Example.com ", "Staro-geslo1")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("prisma-uporabnik"))
                .andExpect(jsonPath("$.canManagePlan").value(true))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void wrongPasswordUnknownEmailAndInactiveUserGetSameAnswer() throws Exception {
        for (String[] emailAndPassword : new String[][] {
                {"stari@example.com", "Napacno-geslo1"},
                {"ne-obstaja@example.com", "Staro-geslo1"},
                {"onemogocen@example.com", "Staro-geslo1"}}) {
            login(emailAndPassword[0], emailAndPassword[1])
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.detail").value("Napačen email ali geslo."));
        }
    }

    @Test
    void passwordChangedByApiWorksForLogin() throws Exception {
        mockMvc.perform(authorized(put("/api/v1/users/prisma-uporabnik/password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"password": "Novo-geslo2!"}
                                """))
                .andExpect(status().isNoContent());

        login("stari@example.com", "Staro-geslo1").andExpect(status().isUnauthorized());
        login("stari@example.com", "Novo-geslo2!").andExpect(status().isOk());
    }

    @Test
    void tooLongPasswordIsRejectedWithoutError() throws Exception {
        login("stari@example.com", "A1!" + "x".repeat(100)).andExpect(status().isUnauthorized());
    }

    @Test
    void loginRequiresApiToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "stari@example.com", "password": "Staro-geslo1"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Manjka ali neveljaven API žeton."));
    }

}
