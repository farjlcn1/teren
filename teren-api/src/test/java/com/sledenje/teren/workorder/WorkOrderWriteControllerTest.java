package com.sledenje.teren.workorder;

import static com.sledenje.teren.TestcontainersConfiguration.AUTHORIZATION_HEADER_VALUE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.sledenje.teren.TestcontainersConfiguration;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import org.hamcrest.Matchers;
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
class WorkOrderWriteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void prepareData() {
        jdbcTemplate.execute("TRUNCATE TABLE \"work_orders\", \"plan_groups\", \"clients\", \"users\", \"vehicles\", "
                + "\"daily_sequence\" CASCADE");
        jdbcTemplate.execute("""
                INSERT INTO "users" ("id", "email", "passwordHash", "fullName", "updatedAt") VALUES
                  ('u-ana', 'ana@example.com', 'x', 'Ana Monter', now()),
                  ('u-bor', 'bor@example.com', 'x', 'Bor Pisarna', now());
                INSERT INTO "clients" ("id", "name", "source", "updatedAt") VALUES
                  ('c-alfa', 'Alfa d.o.o.', 'MANUAL', now()),
                  ('c-beta', 'Beta s.p.', 'MANUAL', now());
                INSERT INTO "vehicles" ("id", "plate", "source", "updatedAt") VALUES ('v-1', 'LJ OBSTOJ-1', 'MANUAL', now());
                INSERT INTO "plan_groups" ("id", "clientId", "startAt", "endAt", "createdById", "updatedAt") VALUES
                  ('pg-alfa', 'c-alfa', now(), now() + interval '2 hours', 'u-bor', now());
                INSERT INTO "planned_tasks" ("id", "planGroupId", "vehiclePlate", "updatedAt") VALUES
                  ('pt-alfa', 'pg-alfa', 'LJ PLAN-1', now());
                """);
    }

    private static MockHttpServletRequestBuilder authorized(MockHttpServletRequestBuilder request) {
        return request.header(HttpHeaders.AUTHORIZATION, AUTHORIZATION_HEADER_VALUE).contentType(MediaType.APPLICATION_JSON);
    }

    private static String details(String overrides) {
        String base = """
                "type": "MONTAZA", "difficulty": "OSNOVNA", "clientId": "c-beta",
                "vehiclePlate": " lj nov-1", "vehicleBrand": "Škoda", "vehicleModel": "Octavia", "vehicleYear": 2020,
                "imei": "1234567890", "imeiPrev": "", "culprit": null, "comment": "Prvi nalog",
                "installers": [{"name": "SIMON"}], "options": [{"optionType": "DIN1", "comment": "vžig"}]
                """;
        return "{" + base + (overrides.isEmpty() ? "" : ", " + overrides) + "}";
    }

    private ResultActions create(String detailsJson, String extra) throws Exception {
        return mockMvc.perform(authorized(post("/api/v1/work-orders")).content("""
                {"details": %s, "deviceModels": [{"deviceModel": "FMC130"}], "createdById": "u-ana"%s}
                """.formatted(detailsJson, extra)));
    }

    private String createAndGetId(String detailsJson, String extra) throws Exception {
        String body = create(detailsJson, extra).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    @Test
    void createAssignsDailyIdentAndStoresChildren() throws Exception {
        String today = LocalDate.now(ZoneId.of("Europe/Ljubljana")).format(DateTimeFormatter.ofPattern("ddMMyyyy"));

        create(details(""), "")
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", Matchers.containsString("/api/v1/work-orders/")))
                .andExpect(jsonPath("$.ident").value(today + "001"));
        String secondId = createAndGetId(details(""), "");

        mockMvc.perform(get("/api/v1/work-orders/" + secondId).header(HttpHeaders.AUTHORIZATION, AUTHORIZATION_HEADER_VALUE))
                .andExpect(jsonPath("$.ident").value(today + "002"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.vehiclePlate").value(" lj nov-1"))
                .andExpect(jsonPath("$.imeiPrev").doesNotExist())
                .andExpect(jsonPath("$.createdBy.id").value("u-ana"))
                .andExpect(jsonPath("$.installers[0].name").value("SIMON"))
                .andExpect(jsonPath("$.options[0].comment").value("vžig"))
                .andExpect(jsonPath("$.deviceModels[0].deviceModel").value("FMC130"));
    }

    @Test
    void createAddsMissingVehicleOnceWithNormalizedPlate() throws Exception {
        createAndGetId(details(""), "");
        createAndGetId(details(""), "");
        createAndGetId(details("\"vehiclePlate\": \"lj obstoj-1\""), "");

        List<String> plates = jdbcTemplate.queryForList("SELECT \"plate\" FROM \"vehicles\" ORDER BY \"plate\"", String.class);
        assertThat(plates).containsExactly("LJ NOV-1", "LJ OBSTOJ-1");
    }

    @Test
    void workOrderWithoutPlanCreatesEventAndNextOneJoinsIt() throws Exception {
        String firstId = createAndGetId(details(""), "");
        String secondId = createAndGetId(details(""), "");

        List<Map<String, Object>> groups = jdbcTemplate.queryForList("""
                SELECT g."id", g."expectedInstaller"::text AS installer, g."createdById", g."startAt", g."endAt"
                FROM "plan_groups" g WHERE g."clientId" = 'c-beta'
                """);
        assertThat(groups).hasSize(1);
        assertThat(groups.getFirst()).containsEntry("installer", "SIMON").containsEntry("createdById", "u-ana");
        Timestamp startAt = (Timestamp) groups.getFirst().get("startAt");
        Timestamp endAt = (Timestamp) groups.getFirst().get("endAt");
        assertThat(endAt.getTime() - startAt.getTime()).isGreaterThanOrEqualTo(60 * 60 * 1000);

        List<String> linkedWorkOrders = jdbcTemplate.queryForList(
                "SELECT \"workOrderId\" FROM \"planned_tasks\" WHERE \"planGroupId\" = ? ORDER BY \"createdAt\"",
                String.class, groups.getFirst().get("id"));
        assertThat(linkedWorkOrders).containsExactly(firstId, secondId);
    }

    @Test
    void otherInstallerGetsOwnEvent() throws Exception {
        createAndGetId(details(""), "");
        createAndGetId(details("\"installers\": [{\"name\": \"OSTALO\", \"otherText\": \"Zunanji\"}]"), "");

        Integer groupCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM \"plan_groups\" WHERE \"clientId\" = 'c-beta'", Integer.class);
        assertThat(groupCount).isEqualTo(2);
        String otherText = jdbcTemplate.queryForObject(
                "SELECT \"expectedInstallerOtherText\" FROM \"plan_groups\" WHERE \"expectedInstaller\" = 'OSTALO'", String.class);
        assertThat(otherText).isEqualTo("Zunanji");
    }

    @Test
    void workOrderFromPlannedTaskCompletesItWithoutNewEvent() throws Exception {
        String workOrderId = createAndGetId(details("\"clientId\": \"c-alfa\""), ", \"plannedTaskId\": \"pt-alfa\"");

        assertThat(jdbcTemplate.queryForObject("SELECT \"workOrderId\" FROM \"planned_tasks\" WHERE \"id\" = 'pt-alfa'",
                String.class)).isEqualTo(workOrderId);
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM \"plan_groups\"", Integer.class)).isEqualTo(1);
    }

    @Test
    void plannedTaskOfOtherClientIsNotLinkedButWorkOrderIsCreated() throws Exception {
        createAndGetId(details(""), ", \"plannedTaskId\": \"pt-alfa\"");

        assertThat(jdbcTemplate.queryForObject("SELECT \"workOrderId\" FROM \"planned_tasks\" WHERE \"id\" = 'pt-alfa'",
                String.class)).isNull();
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM \"work_orders\"", Integer.class)).isEqualTo(1);
    }

    @Test
    void rulesAreEnforcedWithAppMessages() throws Exception {
        create(details("\"type\": \"INTERVENCIJA\""), "").andExpect(status().is(422))
                .andExpect(jsonPath("$.detail").value("Pri intervenciji je polje 'IMEI prej' obvezno."));
        create(details("\"type\": \"INTERVENCIJA\", \"imeiPrev\": \"0987654321\""), "").andExpect(status().is(422))
                .andExpect(jsonPath("$.detail").value("Pri intervenciji je polje 'Krivec' obvezno."));
        create(details("\"imei\": \"12345\""), "").andExpect(status().is(422))
                .andExpect(jsonPath("$.detail").value("IMEI mora vsebovati natanko zadnjih 10 številk."));
        create(details("\"installers\": []"), "").andExpect(status().is(422))
                .andExpect(jsonPath("$.detail").value("Izberi vsaj enega monterja."));
        create(details("\"installers\": [{\"name\": \"OSTALO\", \"otherText\": \" \"}]"), "").andExpect(status().is(422))
                .andExpect(jsonPath("$.detail").value("Vnesi ime monterja pri izbiri 'Ostalo'."));
        create(details("\"vehicleYear\": 1949"), "").andExpect(status().is(422))
                .andExpect(jsonPath("$.detail").value("Neveljavno leto."));
        create(details("\"clientId\": \"ne-obstaja\""), "").andExpect(status().is(422))
                .andExpect(jsonPath("$.detail").value("Stranka ne obstaja."));
        create(details("\"options\": [{\"optionType\": \"DIN1\"}, {\"optionType\": \"DIN1\"}]"), "").andExpect(status().is(422))
                .andExpect(jsonPath("$.detail").value("Opcija DIN1 je naveden večkrat."));
        create(details("\"vehicleBrand\": \"\""), "").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(Matchers.containsString("Vnesi znamko vozila.")));

        mockMvc.perform(authorized(post("/api/v1/work-orders")).content("""
                {"details": %s, "deviceModels": [{"deviceModel": "OSTALO", "comment": ""}], "createdById": "u-ana"}
                """.formatted(details("")))).andExpect(status().is(422))
                .andExpect(jsonPath("$.detail").value("Vnesi napravo pri izbiri 'Drugo'."));

        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM \"work_orders\"", Integer.class)).isZero();
    }

    @Test
    void updateRecordsChangesAndReplacesInstallersAndOptions() throws Exception {
        String workOrderId = createAndGetId(details(""), "");

        mockMvc.perform(authorized(put("/api/v1/work-orders/" + workOrderId)).content("""
                {"details": %s, "editedById": "u-bor"}
                """.formatted(details("""
                "imei": "1111111111", "vehicleYear": 2021, "comment": "",
                "installers": [{"name": "VITO"}, {"name": "SIMON"}],
                "options": [{"optionType": "DIN1", "comment": "nov komentar"}]
                """))))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/work-orders/" + workOrderId).header(HttpHeaders.AUTHORIZATION, AUTHORIZATION_HEADER_VALUE))
                .andExpect(jsonPath("$.imei").value("1111111111"))
                .andExpect(jsonPath("$.comment").doesNotExist())
                .andExpect(jsonPath("$.installers[*].name", Matchers.contains("SIMON", "VITO")))
                .andExpect(jsonPath("$.options[0].comment").value("nov komentar"))
                .andExpect(jsonPath("$.deviceModels[0].deviceModel").value("FMC130"))
                .andExpect(jsonPath("$.edits[0].editedBy.fullName").value("Bor Pisarna"));

        String changesJson = jdbcTemplate.queryForObject(
                "SELECT \"changesJson\" FROM \"work_order_edits\" WHERE \"workOrderId\" = ?", String.class, workOrderId);
        assertThat(changesJson).isEqualTo("{\"Letnik\":{\"from\":2020,\"to\":2021},"
                + "\"IMEI\":{\"from\":\"1234567890\",\"to\":\"1111111111\"},"
                + "\"Komentar\":{\"from\":\"Prvi nalog\",\"to\":null},"
                + "\"Monterji\":{\"from\":\"SIMON:\",\"to\":\"SIMON:,VITO:\"},"
                + "\"DIN/ANI/CAN\":{\"from\":\"DIN1:vžig\",\"to\":\"DIN1:nov komentar\"}}");
    }

    @Test
    void updateWithoutChangesWritesNoHistory() throws Exception {
        String workOrderId = createAndGetId(details(""), "");

        mockMvc.perform(authorized(put("/api/v1/work-orders/" + workOrderId))
                        .content("{\"details\": %s, \"editedById\": \"u-bor\"}".formatted(details(""))))
                .andExpect(status().isNoContent());

        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM \"work_order_edits\"", Integer.class)).isZero();
    }

    @Test
    void updateUnknownWorkOrderIsNotFound() throws Exception {
        mockMvc.perform(authorized(put("/api/v1/work-orders/ne-obstaja"))
                        .content("{\"details\": %s, \"editedById\": \"u-bor\"}".formatted(details(""))))
                .andExpect(status().isNotFound());
    }

    @Test
    void attachmentsAddPhotosAndSignatureLocks() throws Exception {
        String workOrderId = createAndGetId(details(""), "");

        mockMvc.perform(authorized(post("/api/v1/work-orders/" + workOrderId + "/attachments")).content("""
                {"signatureUrl": "work-orders/%1$s/podpis.png",
                 "photos": [{"filePath": "work-orders/%1$s/foto-0.jpg", "takenAt": "2026-10-21T07:55:00Z"},
                            {"filePath": "work-orders/%1$s/foto-1.jpg"}]}
                """.formatted(workOrderId)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/work-orders/" + workOrderId).header(HttpHeaders.AUTHORIZATION, AUTHORIZATION_HEADER_VALUE))
                .andExpect(jsonPath("$.signatureUrl").value("work-orders/" + workOrderId + "/podpis.png"))
                .andExpect(jsonPath("$.signedAt").exists())
                .andExpect(jsonPath("$.lockedAt").exists())
                .andExpect(jsonPath("$.photos", Matchers.hasSize(2)))
                .andExpect(jsonPath("$.photos[0].takenAt").value("2026-10-21T07:55:00Z"));

        mockMvc.perform(authorized(post("/api/v1/work-orders/ne-obstaja/attachments")).content("{\"photos\": []}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void markSentSetsSentAt() throws Exception {
        String workOrderId = createAndGetId(details(""), "");

        mockMvc.perform(authorized(post("/api/v1/work-orders/" + workOrderId + "/sent")))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/work-orders/" + workOrderId).header(HttpHeaders.AUTHORIZATION, AUTHORIZATION_HEADER_VALUE))
                .andExpect(jsonPath("$.sentAt").exists());
    }

}
