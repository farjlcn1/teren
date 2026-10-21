package com.sledenje.teren.common.infrastructure.persistence;

import java.util.Collection;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Prehod sheme s Prisme na Flyway.
 * <ul>
 *     <li>Prazna baza: Flyway izvede vse migracije (V1 je shema iz Prisme).</li>
 *     <li>Baza, ki jo je ustvarila Prisma (tabela {@code _prisma_migrations}, brez zgodovine Flywaya): če ima vse
 *     Prisma migracije, se označi kot V1 (baseline) in se ne spreminja; sicer se API ne zažene.</li>
 *     <li>Baza z zgodovino Flywaya: izvedejo se samo nove migracije.</li>
 * </ul>
 * Neprazna baza brez obeh zgodovin (npr. tuje tabele) ustavi zagon z napako Flywaya, ker baseline-on-migrate ni vklopljen.
 */
@Component
public class PrismaBaselineMigrationStrategy implements FlywayMigrationStrategy {

    private static final Logger log = LoggerFactory.getLogger(PrismaBaselineMigrationStrategy.class);

    /**
     * Zadnja Prisma migracija, ki je zajeta v V1.
     */
    static final String LAST_PRISMA_MIGRATION = "20260928120000_move_installer_to_plan_group";

    @Override
    public void migrate(Flyway flyway) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(flyway.getConfiguration().getDataSource());
        boolean hasFlywayHistory = tableExists(jdbcTemplate, flyway.getConfiguration().getTable());
        if (!hasFlywayHistory && tableExists(jdbcTemplate, "_prisma_migrations")) {
            requireCompletePrismaSchema(jdbcTemplate.queryForList("""
                    SELECT "migration_name" FROM "_prisma_migrations"
                    WHERE "finished_at" IS NOT NULL AND "rolled_back_at" IS NULL
                    """, String.class));
            log.info("Baza ima shemo iz Prisme z vsemi migracijami; označujem jo kot Flyway V1 brez sprememb.");
            flyway.baseline();
        }
        flyway.migrate();
    }

    /**
     * @throws IllegalStateException če zadnja Prisma migracija ni bila uspešno izvedena; baze ne smemo označiti
     *                               kot V1, ker ji manjkajo tabele ali stolpci
     */
    static void requireCompletePrismaSchema(Collection<String> appliedPrismaMigrations) {
        if (!appliedPrismaMigrations.contains(LAST_PRISMA_MIGRATION)) {
            throw new IllegalStateException("Baza ima shemo iz Prisme, ki ni na zadnji verziji (manjka migracija "
                    + LAST_PRISMA_MIGRATION + "). Najprej jo posodobi s staro aplikacijo teren "
                    + "(prisma migrate deploy), nato znova zaženi API.");
        }
    }

    private static boolean tableExists(JdbcTemplate jdbcTemplate, String tableName) {
        // to_regclass išče v trenutni shemi (search_path); ime v narekovajih ohrani velike in male črke.
        Boolean exists = jdbcTemplate.queryForObject(
                "SELECT to_regclass(?) IS NOT NULL", Boolean.class, "\"" + tableName + "\"");
        return Boolean.TRUE.equals(exists);
    }

}
