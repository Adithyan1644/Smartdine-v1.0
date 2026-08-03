package com.smartdine.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * =====================================================================
 * SmartDine — Dual GCP Cloud SQL Routing DataSource
 * =====================================================================
 * ACTIVE ONLY when Spring profile is "prod" (App Engine deployment).
 * Completely INACTIVE on local POS machines (uses local PostgreSQL).
 *
 * Creates TWO explicit HikariCP pools and wires them into a single
 * AbstractRoutingDataSource which Spring JPA uses for all operations.
 * HikariDataSource is configured explicitly (NOT via @ConfigurationProperties)
 * to guarantee the Socket Factory JDBC URL is always applied.
 *
 * Routing (per HTTP request thread via ThreadLocal):
 *   "DEV"  → SmartDine-DEV-Pool  → smartdine_dev (GCP sandbox)
 *   "PROD" → SmartDine-PROD-Pool → smartdine     (GCP production)
 *   null   → PROD (App Engine default) or DEV (local default)
 *
 * Registration routing:
 *   AuthController sets DataSourceContextHolder BEFORE the @Transactional
 *   boundary so RoutingDataSource sees the correct key when Spring opens
 *   the database connection.
 * =====================================================================
 */
@Configuration
@Profile("prod")   // ⚠️ ONLY active on App Engine — NEVER on local POS
public class DataSourceConfig {

    // ── Cloud SQL instance connection name ──────────────────────────────────
    private static final String CLOUD_SQL_INSTANCE =
            "smartdine-saas:asia-south1:smartdine-db";

    // ── Socket Factory class (JDBC connector for App Engine) ────────────────
    private static final String SOCKET_FACTORY =
            "com.google.cloud.sql.postgres.SocketFactory";

    private static final String USERNAME = "postgres";
    private static final String PASSWORD = "admin123";

    // ── JDBC URL builder ────────────────────────────────────────────────────
    private static String jdbcUrl(String dbName) {
        return "jdbc:postgresql:///" + dbName
                + "?socketFactory=" + SOCKET_FACTORY
                + "&cloudSqlInstance=" + CLOUD_SQL_INSTANCE;
    }

    /**
     * DEV datasource — GCP Cloud SQL: smartdine_dev (sandbox).
     * Used for 🧪 Testing / Demo account registrations and queries.
     * Explicit HikariDataSource construction avoids @ConfigurationProperties
     * binding issues that caused silent fallback to default pool.
     */
    @Bean("devDataSource")
    public DataSource devDataSource() {
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(jdbcUrl("smartdine_dev"));
        ds.setUsername(USERNAME);
        ds.setPassword(PASSWORD);
        ds.setDriverClassName("org.postgresql.Driver");
        ds.setPoolName("SmartDine-DEV-Pool");
        ds.setMaximumPoolSize(5);
        ds.setMinimumIdle(1);
        ds.setConnectionTimeout(30_000);
        ds.setIdleTimeout(600_000);
        System.out.println("[DataSourceConfig] DEV pool configured → smartdine_dev (" + CLOUD_SQL_INSTANCE + ")");
        return ds;
    }

    /**
     * PROD datasource — GCP Cloud SQL: smartdine (production).
     * Used for 🟢 Live Production account registrations and queries.
     */
    @Bean("prodDataSource")
    public DataSource prodDataSource() {
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(jdbcUrl("smartdine"));
        ds.setUsername(USERNAME);
        ds.setPassword(PASSWORD);
        ds.setDriverClassName("org.postgresql.Driver");
        ds.setPoolName("SmartDine-PROD-Pool");
        ds.setMaximumPoolSize(10);
        ds.setMinimumIdle(2);
        ds.setConnectionTimeout(30_000);
        ds.setIdleTimeout(600_000);
        System.out.println("[DataSourceConfig] PROD pool configured → smartdine (" + CLOUD_SQL_INSTANCE + ")");
        return ds;
    }

    /**
     * Primary routing datasource.
     * Marked @Primary so Spring Boot JPA auto-wires this as the single
     * DataSource for EntityManagerFactory and TransactionManager.
     * Physical DB connection is selected per-request via RoutingDataSource
     * which reads DataSourceContextHolder (ThreadLocal) at connection time.
     */
    @Primary
    @Bean("dataSource")
    public DataSource dataSource(
            @Qualifier("devDataSource")  DataSource devDs,
            @Qualifier("prodDataSource") DataSource prodDs) {

        Map<Object, Object> targets = new LinkedHashMap<>();
        targets.put(DataSourceContextHolder.DEV,  devDs);
        targets.put(DataSourceContextHolder.PROD, prodDs);

        RoutingDataSource routing = new RoutingDataSource();
        routing.setDefaultTargetDataSource(prodDs); // App Engine default → PROD
        routing.setTargetDataSources(targets);
        // Ensure both DEV and PROD databases have mandatory schema columns (e.g. app_users.phone)
        autoUpdateSchema(devDs, "DEV");
        autoUpdateSchema(prodDs, "PROD");

        System.out.println("[DataSourceConfig] ✅ Dual GCP Cloud SQL routing ACTIVE:");
        System.out.println("   DEV  (SmartDine-DEV-Pool)  → smartdine_dev  (sandbox)");
        System.out.println("   PROD (SmartDine-PROD-Pool) → smartdine       (production)");

        return routing;
    }

    private void autoUpdateSchema(DataSource ds, String name) {
        try (java.sql.Connection conn = ds.getConnection();
             java.sql.Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("ALTER TABLE app_users ADD COLUMN IF NOT EXISTS phone VARCHAR(50)");
            System.out.println("[DataSourceConfig] ✅ Schema verified for " + name + " (app_users.phone)");
        } catch (Exception e) {
            System.err.println("[DataSourceConfig] Schema update notice for " + name + ": " + e.getMessage());
        }
    }
}
