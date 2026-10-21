package com.sledenje.teren;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Testi tečejo na lastni, začasni bazi v Dockerju (ista verzija kot v docker-compose.yml).
 * Shema se ustvari iz db/teren-schema.sql, ki je sestavljena iz Prisma migracij.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    /**
     * API žeton v testih. Zahtevki na {@code /api/**} ga pošljejo v glavi {@code Authorization}.
     */
    public static final String API_TOKEN = "testni-api-zeton";

    public static final String AUTHORIZATION_HEADER_VALUE = "Bearer " + API_TOKEN;

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer(DockerImageName.parse("postgres:17-alpine"))
                .withInitScript("db/teren-schema.sql");
    }

    @Bean
    DynamicPropertyRegistrar apiTokenProperties() {
        return registry -> registry.add("teren.api.token", () -> API_TOKEN);
    }

}
