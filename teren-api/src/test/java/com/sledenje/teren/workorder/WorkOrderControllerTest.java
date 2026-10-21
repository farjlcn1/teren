package com.sledenje.teren.workorder;

import static com.sledenje.teren.TestcontainersConfiguration.AUTHORIZATION_HEADER_VALUE;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class WorkOrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Trije nalogi v obliki, kot jih zapiše Prisma: wo-1 (Beta, Ana, z monterji, opcijami, fotografijami
     * in urejanji), wo-2 (Alfa, Bor) in wo-3 (Alfa, Ana, preklican, isti IMEI kot wo-1).
     */
    @BeforeAll
    void prepareWorkOrders() {
        jdbcTemplate.execute("TRUNCATE TABLE \"work_orders\", \"clients\", \"users\" CASCADE");
        jdbcTemplate.execute("""
                INSERT INTO "users" ("id", "email", "passwordHash", "fullName", "updatedAt") VALUES
                  ('u-ana', 'ana@example.com', 'x', 'Ana Monter', now()),
                  ('u-bor', 'bor@example.com', 'x', 'Bor Pisarna', now());
                INSERT INTO "clients" ("id", "name", "address", "contactInfo", "source", "updatedAt") VALUES
                  ('c-alfa', 'Alfa d.o.o.', 'Alfa cesta 1', '01 111 111', 'MANUAL', now()),
                  ('c-beta', 'Beta s.p.', NULL, NULL, 'IMPORT', now());
                INSERT INTO "work_orders" ("id", "ident", "orderDate", "type", "difficulty", "status", "clientId",
                    "vehiclePlate", "vehicleBrand", "vehicleModel", "vehicleYear", "imei", "imeiPrev", "culprit",
                    "comment", "signatureUrl", "signedAt", "createdById", "createdAt", "updatedAt") VALUES
                  ('wo-1', '20261001-001', '2026-10-01 08:00:00', 'MONTAZA', 'OSNOVNA', 'ACTIVE', 'c-beta',
                   'LJ 100_A', 'Škoda', 'Octavia', 2020, '350000000000001', NULL, NULL,
                   'Komentar', 'signatures/wo-1.png', '2026-10-01 09:00:00', 'u-ana', now(), now()),
                  ('wo-2', '20261002-001', '2026-10-02 08:00:00', 'INTERVENCIJA', 'ZAHTEVNA', 'ACTIVE', 'c-alfa',
                   'MB 200', 'VW', 'Golf', 2018, '350000000000002', '350000000000009', 'STRANKA',
                   NULL, NULL, NULL, 'u-bor', now(), now()),
                  ('wo-3', '20261003-001', '2026-10-03 08:00:00', 'DEMONTAZA', 'OSNOVNA', 'CANCELLED', 'c-alfa',
                   'lj 300', 'Renault', 'Clio', 2015, '350000000000001', NULL, 'SLEDENJE',
                   NULL, NULL, NULL, 'u-ana', now(), now());
                INSERT INTO "work_order_installers" ("id", "workOrderId", "name", "otherText") VALUES
                  ('i-1', 'wo-1', 'OSTALO', 'Zunanji monter'),
                  ('i-2', 'wo-1', 'SIMON', NULL),
                  ('i-3', 'wo-2', 'VITO', NULL);
                INSERT INTO "work_order_options" ("id", "workOrderId", "optionType", "comment") VALUES
                  ('o-1', 'wo-1', 'ALL_CAN', NULL),
                  ('o-2', 'wo-1', 'DIN1', 'vžig'),
                  ('o-3', 'wo-2', 'TACHO', NULL);
                INSERT INTO "work_order_device_models" ("id", "workOrderId", "deviceModel", "comment") VALUES
                  ('d-1', 'wo-1', 'FMC130', NULL),
                  ('d-2', 'wo-2', 'FMC650', 'nova');
                INSERT INTO "work_order_photos" ("id", "workOrderId", "filePath", "takenAt", "uploadedAt") VALUES
                  ('p-2', 'wo-1', 'photos/wo-1-b.jpg', NULL, '2026-10-01 08:20:00'),
                  ('p-1', 'wo-1', 'photos/wo-1-a.jpg', '2026-10-01 07:55:00', '2026-10-01 08:10:00');
                INSERT INTO "work_order_edits" ("id", "workOrderId", "editedById", "editedAt", "changesJson") VALUES
                  ('e-1', 'wo-1', 'u-bor', '2026-10-01 10:00:00', '{"imei":["1","2"]}'),
                  ('e-2', 'wo-1', 'u-ana', '2026-10-01 11:00:00', '{"comment":["","Komentar"]}');
                """);
    }

    private ResultActions getWorkOrders(String query) throws Exception {
        return mockMvc.perform(get("/api/v1/work-orders" + query)
                .header(HttpHeaders.AUTHORIZATION, AUTHORIZATION_HEADER_VALUE));
    }

    @Test
    void listIsNewestFirstWithClientAuthorAndChildren() throws Exception {
        getWorkOrders("")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", contains("wo-3", "wo-2", "wo-1")))
                .andExpect(jsonPath("$[2].ident").value("20261001-001"))
                .andExpect(jsonPath("$[2].orderDate").value("2026-10-01T08:00:00Z"))
                .andExpect(jsonPath("$[2].client.name").value("Beta s.p."))
                .andExpect(jsonPath("$[2].createdBy.fullName").value("Ana Monter"))
                .andExpect(jsonPath("$[2].installers[*].name", contains("SIMON", "OSTALO")))
                .andExpect(jsonPath("$[2].installers[1].otherText").value("Zunanji monter"))
                .andExpect(jsonPath("$[2].installers[1].id").value("i-1"))
                .andExpect(jsonPath("$[2].options[*].optionType", contains("DIN1", "ALL_CAN")))
                .andExpect(jsonPath("$[2].options[0].comment").value("vžig"))
                .andExpect(jsonPath("$[2].deviceModels[0].deviceModel").value("FMC130"))
                .andExpect(jsonPath("$[2].signatureUrl").value("signatures/wo-1.png"))
                .andExpect(jsonPath("$[2].photos").doesNotExist())
                .andExpect(jsonPath("$[0].status").value("CANCELLED"))
                .andExpect(jsonPath("$[0].installers", hasSize(0)));
    }

    @Test
    void sortsByClientNameWithIdAsTieBreaker() throws Exception {
        getWorkOrders("?sort=client&dir=asc")
                .andExpect(jsonPath("$[*].id", contains("wo-2", "wo-3", "wo-1")));
        getWorkOrders("?sort=client&dir=desc")
                .andExpect(jsonPath("$[*].id", contains("wo-1", "wo-2", "wo-3")));
    }

    @Test
    void sortsByOtherFields() throws Exception {
        getWorkOrders("?sort=ident&dir=asc").andExpect(jsonPath("$[*].id", contains("wo-1", "wo-2", "wo-3")));
        // Enum se ureja po vrstnem redu v Postgresu: MONTAZA, DEMONTAZA, INTERVENCIJA.
        getWorkOrders("?sort=type&dir=asc").andExpect(jsonPath("$[*].id", contains("wo-1", "wo-3", "wo-2")));
        getWorkOrders("?sort=neznano&dir=asc").andExpect(jsonPath("$[*].id", contains("wo-1", "wo-2", "wo-3")));
    }

    @Test
    void vehiclePlateFilterIgnoresCaseAndTreatsWildcardsLiterally() throws Exception {
        getWorkOrders("?vehiclePlate=LJ").andExpect(jsonPath("$[*].id", contains("wo-3", "wo-1")));
        // "_" je dobesedni znak, ne nadomestni (sicer bi se "0_" ujemal tudi z "MB 200" in "lj 300").
        getWorkOrders("?vehiclePlate=0_").andExpect(jsonPath("$[*].id", contains("wo-1")));
    }

    @Test
    void filtersByChildCollectionsWithoutDuplicates() throws Exception {
        getWorkOrders("?installer=OSTALO").andExpect(jsonPath("$[*].id", contains("wo-1")));
        getWorkOrders("?option=TACHO").andExpect(jsonPath("$[*].id", contains("wo-2")));
        getWorkOrders("?deviceModel=FMC130").andExpect(jsonPath("$[*].id", contains("wo-1")));
    }

    @Test
    void filtersByScalarFields() throws Exception {
        getWorkOrders("?imei=0001").andExpect(jsonPath("$[*].id", contains("wo-3", "wo-1")));
        getWorkOrders("?type=DEMONTAZA").andExpect(jsonPath("$[*].id", contains("wo-3")));
        getWorkOrders("?difficulty=ZAHTEVNA").andExpect(jsonPath("$[*].id", contains("wo-2")));
        getWorkOrders("?culprit=STRANKA").andExpect(jsonPath("$[*].id", contains("wo-2")));
        getWorkOrders("?clientId=c-alfa").andExpect(jsonPath("$[*].id", contains("wo-3", "wo-2")));
        getWorkOrders("?createdById=u-ana").andExpect(jsonPath("$[*].id", contains("wo-3", "wo-1")));
        getWorkOrders("?from=2026-10-02T00:00:00Z&to=2026-10-02T23:59:59.999Z")
                .andExpect(jsonPath("$[*].id", contains("wo-2")));
        getWorkOrders("?type=MONTAZA&createdById=u-bor").andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void limitRestrictsNumberOfResults() throws Exception {
        getWorkOrders("?limit=2").andExpect(jsonPath("$[*].id", contains("wo-3", "wo-2")));
        getWorkOrders("?limit=0").andExpect(status().isBadRequest());
    }

    @Test
    void invalidEnumValueIsBadRequest() throws Exception {
        getWorkOrders("?type=NEOBSTOJEC").andExpect(status().isBadRequest());
    }

    @Test
    void detailHasWorkOrderFieldsOnTopLevelWithPhotosAndEdits() throws Exception {
        mockMvc.perform(get("/api/v1/work-orders/wo-1").header(HttpHeaders.AUTHORIZATION, AUTHORIZATION_HEADER_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("wo-1"))
                .andExpect(jsonPath("$.ident").value("20261001-001"))
                .andExpect(jsonPath("$.client.id").value("c-beta"))
                .andExpect(jsonPath("$.workOrder").doesNotExist())
                .andExpect(jsonPath("$.photos[*].id", contains("p-1", "p-2")))
                .andExpect(jsonPath("$.photos[0].takenAt").value("2026-10-01T07:55:00Z"))
                .andExpect(jsonPath("$.photos[1].takenAt").doesNotExist())
                .andExpect(jsonPath("$.edits[*].id", contains("e-2", "e-1")))
                .andExpect(jsonPath("$.edits[1].editedBy.fullName").value("Bor Pisarna"))
                .andExpect(jsonPath("$.edits[1].changesJson").value("{\"imei\":[\"1\",\"2\"]}"));
    }

    @Test
    void unknownWorkOrderIsNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/work-orders/ne-obstaja").header(HttpHeaders.AUTHORIZATION, AUTHORIZATION_HEADER_VALUE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value(containsString("ne obstaja")));
    }

    @Test
    void imeisAreDistinctAndSorted() throws Exception {
        mockMvc.perform(get("/api/v1/work-orders/imeis").header(HttpHeaders.AUTHORIZATION, AUTHORIZATION_HEADER_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", contains("350000000000001", "350000000000002")));
    }

    @Test
    void requiresApiToken() throws Exception {
        mockMvc.perform(get("/api/v1/work-orders")).andExpect(status().isUnauthorized());
    }

}
