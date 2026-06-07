package com.paymentswitch.persistence.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.util.Properties;

/**
 * Programmatic HikariCP + PostgreSQL DataSource configuration.
 *
 * <p>All values default to environment variables with sensible fallbacks.
 * Override by providing a {@link Properties} object at construction time.</p>
 *
 * <pre>
 *   ENV vars recognised:
 *     DB_HOST       (default: localhost)
 *     DB_PORT       (default: 5432)
 *     DB_NAME       (default: payment_switch)
 *     DB_USER       (default: switch_user)
 *     DB_PASSWORD   (required)
 *     DB_POOL_SIZE  (default: 10)
 * </pre>
 */
public class DataSourceConfig {

    private static final Logger log = LoggerFactory.getLogger(DataSourceConfig.class);

    private final Properties props;

    public DataSourceConfig() {
        this(new Properties());
    }

    public DataSourceConfig(Properties overrides) {
        this.props = overrides;
    }

    public DataSource build() {
        HikariConfig config = new HikariConfig();

        String host     = get("DB_HOST",     "localhost");
        String port     = get("DB_PORT",     "5432");
        String dbName   = get("DB_NAME",     "payment_switch");
        String user     = get("DB_USER",     "switch_user");
        String password = get("DB_PASSWORD", "");
        int poolSize    = Integer.parseInt(get("DB_POOL_SIZE", "10"));

        config.setJdbcUrl("jdbc:postgresql://" + host + ":" + port + "/" + dbName
                + "?ApplicationName=payment-switch&reWriteBatchedInserts=true");
        config.setUsername(user);
        config.setPassword(password);
        config.setMaximumPoolSize(poolSize);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(5_000);
        config.setIdleTimeout(300_000);
        config.setMaxLifetime(1_800_000);
        config.setPoolName("SwitchPool");

        // PostgreSQL-specific tuning
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");

        log.info("DataSource configured: {}:{}/{} pool={}", host, port, dbName, poolSize);
        return new HikariDataSource(config);
    }

    private String get(String key, String defaultValue) {
        // Props override, then env var, then default
        if (props.containsKey(key))       return props.getProperty(key);
        String env = System.getenv(key);
        return (env != null && !env.isEmpty()) ? env : defaultValue;
    }
}
