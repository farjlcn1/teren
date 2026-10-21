package com.sledenje.teren.plan;

import static com.sledenje.teren.TestcontainersConfiguration.AUTHORIZATION_HEADER_VALUE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.sledenje.teren.TestcontainersConfiguration;
import java.util.Map;
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
class PlanControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 20. 10. 2026 (UTC): g-simon-1 10:00–11:00 in g-simon-2 11:00–12:00 (Simon, Alfa), g-vito 10:00–11:00 (Vito, Beta,
     * z opravljenim nalogom), g-ostalo 13:00–14:00 ("Ostalo": Zunanji). 21. 10.: g-naslednji dan.
     */
    @BeforeEach
    void preparePlan() {
        jdbcTemplate.execute("TRUNCATE TABLE \"work_orders\", \"plan_groups\", \"clients\", \"users\" CASCADE");
        jdbcTemplate.execute("""
                INSERT INTO "users" ("id", "email", "passwordHash", "fullName", "updatedAt")
                  VALUES ('u-ana', 'ana@example.com', 'x', 'Ana', now());
                INSERT INTO "clients" ("id", "name", "source", "updatedAt") VALUES
                  ('c-alfa', 'Alfa d.o.o.', 'MANUAL', now()), ('c-beta', 'Beta s.p.', 'MANUAL', now());
                INSERT INTO "work_orders" ("id", "ident", "type", "difficulty", "clientId", "vehiclePlate", "vehicleBrand",
                    "vehicleModel", "vehicleYear", "imei", "createdById", "updatedAt")
                  VALUES ('wo-1', '20102026001', 'MONTAZA', 'OSNOVNA', 'c-beta', 'LJ 1', 'VW', 'Golf', 2020, '1234567890', 'u-ana', now());
                INSERT INTO "plan_groups" ("id", "clientId", "startAt", "endAt", "expectedInstaller", "expectedInstallerOtherText",
                    "note", "createdById", "updatedAt") VALUES
                  ('g-simon-1', 'c-alfa', '2026-10-20 10:00', '2026-10-20 11:00', 'SIMON', NULL, 'Prvi', 'u-ana', now()),
                  ('g-simon-2', 'c-alfa', '2026-10-20 11:00', '2026-10-20 12:00', 'SIMON', NULL, NULL, 'u-ana', now()),
                  ('g-vito', 'c-beta', '2026-10-20 10:00', '2026-10-20 11:00', 'VITO', NULL, NULL, 'u-ana', now()),
                  ('g-ostalo', 'c-beta', '2026-10-20 13:00', '2026-10-20 14:00', 'OSTALO', 'Zunanji', NULL, 'u-ana', now()),
                  ('g-naslednji', 'c-alfa', '2026-10-21 08:00', '2026-10-21 09:00', NULL, NULL, NULL, 'u-ana', now());
                INSERT INTO "planned_tasks" ("id", "planGroupId", "vehiclePlate", "type", "note", "workOrderId", "createdAt", "updatedAt") VALUES
                  ('t-2', 'g-simon-1', 'LJ DRUGI', NULL, NULL, NULL, '2026-10-19 09:00', now()),
                  ('t-1', 'g-simon-1', 'LJ PRVI', 'MONTAZA', 'opomba', NULL, '2026-10-19 08:00', now()),
                  ('t-vito', 'g-vito', 'LJ 1', 'MONTAZA', NULL, 'wo-1', now(), now());
                """);
    }

    private static MockHttpServletRequestBuilder authorized(MockHttpServletRequestBuilder request) {
        return request.header(HttpHeaders.AUTHORIZATION, AUTHORIZATION_HEADER_VALUE).contentType(MediaType.APPLICATION_JSON);
    }

    private Map<String, Object> window(String planGroupId) {
        return jdbcTemplate.queryForMap("""
                SELECT to_char("startAt", 'DD HH24:MI') AS start, to_char("endAt", 'DD HH24:MI') AS end
                FROM "plan_groups" WHERE "id" = ?
                """, planGroupId);
    }

    @Test
    void listsGroupsOverlappingPeriodWithClientAndTasksInCreationOrder() throws Exception {
        mockMvc.perform(authorized(get("/api/v1/plan-groups?from=2026-10-20T00:00:00Z&to=2026-10-21T00:00:00Z")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", contains("g-simon-1", "g-vito", "g-simon-2", "g-ostalo")))
                .andExpect(jsonPath("$[0].clientName").value("Alfa d.o.o."))
                .andExpect(jsonPath("$[0].startAt").value("2026-10-20T10:00:00Z"))
                .andExpect(jsonPath("$[0].tasks[*].id", contains("t-1", "t-2")))
                .andExpect(jsonPath("$[0].tasks[0].type").value("MONTAZA"))
                .andExpect(jsonPath("$[1].tasks[0].workOrderId").value("wo-1"))
                .andExpect(jsonPath("$[3].expectedInstallerOtherText").value("Zunanji"));
    }

    @Test
    void createWithoutEndLastsOneHourAndShiftsOverlappingEventsOfInstallerInChain() throws Exception {
        String body = mockMvc.perform(authorized(post("/api/v1/plan-groups")).content("""
                        {"clientId": "c-beta", "startAt": "2026-10-20T09:30:00Z", "expectedInstaller": "SIMON",
                         "expectedInstallerOtherText": "prezrto", "note": " ", "createdById": "u-ana",
                         "tasks": [{"vehiclePlate": "KR 5", "type": "INTERVENCIJA"}]}
                        """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String planGroupId = JsonPath.read(body, "$.id");

        assertThat(window(planGroupId)).containsEntry("start", "20 09:30").containsEntry("end", "20 10:30");
        assertThat(jdbcTemplate.queryForMap(
                "SELECT \"expectedInstallerOtherText\" AS other, \"note\" FROM \"plan_groups\" WHERE \"id\" = ?", planGroupId))
                .containsEntry("other", null).containsEntry("note", null);
        // Simonov dogodek 10:00–11:00 gre za konec novega (10:30–11:30), zato tudi naslednji 11:00–12:00 (11:30–12:30).
        assertThat(window("g-simon-1")).containsEntry("start", "20 10:30").containsEntry("end", "20 11:30");
        assertThat(window("g-simon-2")).containsEntry("start", "20 11:30").containsEntry("end", "20 12:30");
        assertThat(window("g-vito")).containsEntry("start", "20 10:00");
    }

    @Test
    void otherInstallerIsComparedByName() throws Exception {
        mockMvc.perform(authorized(post("/api/v1/plan-groups")).content("""
                        {"clientId": "c-alfa", "startAt": "2026-10-20T13:00:00Z", "endAt": "2026-10-20T14:00:00Z",
                         "expectedInstaller": "OSTALO", "expectedInstallerOtherText": "Drugi zunanji", "createdById": "u-ana", "tasks": []}
                        """))
                .andExpect(status().isCreated());
        assertThat(window("g-ostalo")).containsEntry("start", "20 13:00");

        mockMvc.perform(authorized(post("/api/v1/plan-groups")).content("""
                        {"clientId": "c-alfa", "startAt": "2026-10-20T13:00:00Z", "endAt": "2026-10-20T14:00:00Z",
                         "expectedInstaller": "OSTALO", "expectedInstallerOtherText": "Zunanji", "createdById": "u-ana", "tasks": []}
                        """))
                .andExpect(status().isCreated());
        assertThat(window("g-ostalo")).containsEntry("start", "20 14:00").containsEntry("end", "20 15:00");
    }

    @Test
    void createValidatesWithAppMessages() throws Exception {
        String template = """
                {"clientId": "%s", "startAt": "2026-10-22T10:00:00Z", "endAt": "%s", "expectedInstaller": %s,
                 "createdById": "u-ana", "tasks": [{"vehiclePlate": "%s"}]}
                """;
        mockMvc.perform(authorized(post("/api/v1/plan-groups")).content(template.formatted("c-alfa", "2026-10-22T10:00:00Z", "null", "LJ 1")))
                .andExpect(status().is(422)).andExpect(jsonPath("$.detail").value("Konec mora biti po začetku."));
        mockMvc.perform(authorized(post("/api/v1/plan-groups")).content(template.formatted("c-alfa", "2026-10-22T11:00:00Z", "\"OSTALO\"", "LJ 1")))
                .andExpect(status().is(422)).andExpect(jsonPath("$.detail").value("Vnesi ime monterja pri izbiri 'Ostalo'."));
        mockMvc.perform(authorized(post("/api/v1/plan-groups")).content(template.formatted("ne-obstaja", "2026-10-22T11:00:00Z", "null", "LJ 1")))
                .andExpect(status().is(422)).andExpect(jsonPath("$.detail").value("Stranka ne obstaja."));
        mockMvc.perform(authorized(post("/api/v1/plan-groups")).content(template.formatted("c-alfa", "2026-10-22T11:00:00Z", "null", " ")))
                .andExpect(status().isBadRequest());
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM \"plan_groups\"", Integer.class)).isEqualTo(5);
    }

    @Test
    void updateRequiresEndAndKeepsClientOfCompletedEvent() throws Exception {
        mockMvc.perform(authorized(put("/api/v1/plan-groups/g-vito")).content("""
                        {"clientId": "c-alfa", "startAt": "2026-10-20T10:00:00Z", "endAt": "2026-10-20T11:00:00Z", "expectedInstaller": "VITO"}
                        """))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.detail").value("Stranke ni mogoče spremeniti, ker je bil vsaj en nalog v tej skupini že opravljen."));
        mockMvc.perform(authorized(put("/api/v1/plan-groups/g-simon-1")).content("""
                        {"clientId": "c-alfa", "startAt": "2026-10-20T10:00:00Z"}
                        """))
                .andExpect(status().is(422)).andExpect(jsonPath("$.detail").value("Vnesi konec."));
        mockMvc.perform(authorized(put("/api/v1/plan-groups/ne-obstaja")).content("""
                        {"clientId": "c-alfa", "startAt": "2026-10-20T10:00:00Z", "endAt": "2026-10-20T11:00:00Z"}
                        """))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.detail").value("Dogodek ne obstaja."));
    }

    @Test
    void updateChangesDetailsAndShiftsOtherEventsButNotItself() throws Exception {
        mockMvc.perform(authorized(put("/api/v1/plan-groups/g-simon-1")).content("""
                        {"clientId": "c-beta", "startAt": "2026-10-20T10:00:00Z", "endAt": "2026-10-20T11:30:00Z",
                         "note": "Spremenjeno", "contact": "041 111 111", "expectedInstaller": "SIMON"}
                        """))
                .andExpect(status().isNoContent());

        assertThat(jdbcTemplate.queryForMap("SELECT \"clientId\", \"note\", \"contact\" FROM \"plan_groups\" WHERE \"id\" = 'g-simon-1'"))
                .containsEntry("clientId", "c-beta").containsEntry("note", "Spremenjeno").containsEntry("contact", "041 111 111");
        assertThat(window("g-simon-1")).containsEntry("start", "20 10:00").containsEntry("end", "20 11:30");
        assertThat(window("g-simon-2")).containsEntry("start", "20 11:30").containsEntry("end", "20 12:30");
    }

    @Test
    void moveChangesWindowAndShiftsOverlappingEvents() throws Exception {
        mockMvc.perform(authorized(put("/api/v1/plan-groups/g-simon-2/window")).content("""
                        {"startAt": "2026-10-20T09:30:00Z", "endAt": "2026-10-20T10:30:00Z"}
                        """))
                .andExpect(status().isNoContent());

        assertThat(window("g-simon-2")).containsEntry("start", "20 09:30").containsEntry("end", "20 10:30");
        assertThat(window("g-simon-1")).containsEntry("start", "20 10:30").containsEntry("end", "20 11:30");

        mockMvc.perform(authorized(put("/api/v1/plan-groups/g-simon-2/window")).content("""
                        {"startAt": "2026-10-20T10:30:00Z", "endAt": "2026-10-20T10:30:00Z"}
                        """))
                .andExpect(status().is(422));
    }

    @Test
    void completedEventAndTaskCannotBeDeleted() throws Exception {
        mockMvc.perform(authorized(delete("/api/v1/plan-groups/g-vito")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Dogodka ni mogoče izbrisati, ker vsebuje že opravljen nalog."));
        mockMvc.perform(authorized(delete("/api/v1/planned-tasks/t-vito")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Opravljenega naloga ni mogoče izbrisati."));
    }

    @Test
    void deleteEventRemovesItsTasks() throws Exception {
        mockMvc.perform(authorized(delete("/api/v1/plan-groups/g-simon-1"))).andExpect(status().isNoContent());

        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM \"planned_tasks\" WHERE \"id\" IN ('t-1', 't-2')",
                Integer.class)).isZero();
    }

    @Test
    void plannedTasksCanBeAddedReadAndDeleted() throws Exception {
        String body = mockMvc.perform(authorized(post("/api/v1/plan-groups/g-naslednji/tasks")).content("""
                        {"vehiclePlate": "NM 7", "type": null, "note": ""}
                        """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String plannedTaskId = JsonPath.read(body, "$.id");

        mockMvc.perform(authorized(get("/api/v1/planned-tasks/" + plannedTaskId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vehiclePlate").value("NM 7"))
                .andExpect(jsonPath("$.note").doesNotExist())
                .andExpect(jsonPath("$.planGroupId").value("g-naslednji"))
                .andExpect(jsonPath("$.clientId").value("c-alfa"))
                .andExpect(jsonPath("$.clientName").value("Alfa d.o.o."));

        mockMvc.perform(authorized(delete("/api/v1/planned-tasks/" + plannedTaskId))).andExpect(status().isNoContent());
        mockMvc.perform(authorized(get("/api/v1/planned-tasks/" + plannedTaskId)))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.detail").value("Nalog ne obstaja."));
        mockMvc.perform(authorized(post("/api/v1/plan-groups/ne-obstaja/tasks")).content("{\"vehiclePlate\": \"X\"}"))
                .andExpect(status().isNotFound());
    }

}
