package com.sledenje.teren.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sledenje.teren.TestcontainersConfiguration;
import com.sledenje.teren.client.domain.exception.ClientNotFoundException;
import com.sledenje.teren.client.domain.model.Client;
import com.sledenje.teren.client.domain.model.ClientSource;
import com.sledenje.teren.client.domain.model.NewClient;
import com.sledenje.teren.client.domain.service.ClientService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ClientServiceTest {

    @Autowired
    private ClientService clientService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeAll
    void prepareDatabase() {
        cleanClientsTable();
    }

    @BeforeEach
    void cleanBeforeEachTest() {
        cleanClientsTable();
    }

    private void cleanClientsTable() {
        jdbcTemplate.execute("TRUNCATE TABLE \"clients\" CASCADE");
    }

    @Test
    void createdClientCanBeFoundById() {
        Client createdClient = clientService.createClient(
                new NewClient("Prevozi Novak d.o.o.", "Dunajska cesta 1, Ljubljana", null));

        Client foundClient = clientService.findClientById(createdClient.id());

        assertThat(foundClient.name()).isEqualTo("Prevozi Novak d.o.o.");
        assertThat(foundClient.address()).isEqualTo("Dunajska cesta 1, Ljubljana");
        assertThat(foundClient.contactInfo()).isNull();
        assertThat(foundClient.source()).isEqualTo(ClientSource.MANUAL);
        assertThat(foundClient.createdAt()).isNotNull().isEqualTo(foundClient.updatedAt());
    }

    @Test
    void findAllClientsReturnsClientsOrderedByName() {
        clientService.createClient(new NewClient("Zeleni transport", null, null));
        clientService.createClient(new NewClient("Avtoprevozništvo Kos", null, null));

        List<String> clientNames = clientService.findAllClients().stream().map(Client::name).toList();

        assertThat(clientNames).containsExactly("Avtoprevozništvo Kos", "Zeleni transport");
    }

    @Test
    void findClientByIdThrowsForUnknownClient() {
        assertThatThrownBy(() -> clientService.findClientById("ne-obstaja"))
                .isInstanceOf(ClientNotFoundException.class)
                .hasMessageContaining("ne-obstaja");
    }

    @Test
    void readsClientWrittenByPrismaWithUtcTimestamps() {
        // Zapis v obliki, kot ga shrani Prisma: cuid ID, enum vrednost, čas v UTC brez časovnega pasu.
        jdbcTemplate.update("""
                INSERT INTO "clients" ("id", "name", "source", "createdAt", "updatedAt")
                VALUES ('cmd8x2k0q0000abcd1234efgh', 'Uvožena stranka', 'IMPORT',
                        '2026-07-24 07:32:07.123', '2026-07-24 07:32:07.123')
                """);

        Client importedClient = clientService.findClientById("cmd8x2k0q0000abcd1234efgh");

        assertThat(importedClient.source()).isEqualTo(ClientSource.IMPORT);
        assertThat(importedClient.createdAt()).isEqualTo(Instant.parse("2026-07-24T07:32:07.123Z"));
    }

}
