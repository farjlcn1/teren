package com.sledenje.teren.common.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class PrismaBaselineMigrationStrategyTest {

    @Test
    void prismaSchemaWithAllMigrationsCanBeBaselined() {
        assertThatCode(() -> PrismaBaselineMigrationStrategy.requireCompletePrismaSchema(
                List.of("20260724073207_init", PrismaBaselineMigrationStrategy.LAST_PRISMA_MIGRATION)))
                .doesNotThrowAnyException();
    }

    @Test
    void prismaSchemaWithoutLastMigrationStopsStartup() {
        assertThatThrownBy(() -> PrismaBaselineMigrationStrategy.requireCompletePrismaSchema(
                List.of("20260724073207_init", "20260928114347_add_plan_group_contact")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(PrismaBaselineMigrationStrategy.LAST_PRISMA_MIGRATION);
    }

}
