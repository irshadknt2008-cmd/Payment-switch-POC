package com.paymentswitch.persistence.config;

import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;

/**
 * Runs Flyway database migrations on startup.
 * Migration scripts are located in {@code classpath:db/migration}.
 */
public class FlywayMigrator {

    private static final Logger log = LoggerFactory.getLogger(FlywayMigrator.class);

    private final DataSource dataSource;

    public FlywayMigrator(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void migrate() {
        log.info("Running Flyway database migrations...");
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .validateOnMigrate(true)
                .load();

        int applied = flyway.migrate().migrationsExecuted;
        log.info("Flyway migration complete. {} migration(s) applied.", applied);
    }
}
