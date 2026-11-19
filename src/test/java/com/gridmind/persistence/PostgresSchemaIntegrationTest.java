package com.gridmind.persistence;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import com.gridmind.service.OperationalDataRetentionService;
import com.gridmind.entity.CommandLogEntity;
import com.gridmind.entity.CommandSnapshotEntity;
import com.gridmind.repository.CommandLogRepository;
import com.gridmind.repository.CommandSnapshotRepository;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, properties = {
        "gridmind.simulator.enabled=false",
        "gridmind.retention.days=30",
        "spring.jpa.hibernate.ddl-auto=validate"
})
class PostgresSchemaIntegrationTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private Flyway flyway;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private OperationalDataRetentionService retentionService;

    @Autowired
    private CommandLogRepository commandLogRepository;

    @Autowired
    private CommandSnapshotRepository commandSnapshotRepository;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Test
    void appliesAllVersionedMigrationsAndValidatesJpaSchema() {
        assertEquals(0, flyway.info().pending().length);
        assertEquals(2, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'transformers' AND column_name IN ('rating_kva', 'age_years') AND is_nullable = 'YES'",
                Integer.class));
        assertEquals(2, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'simulations' AND column_name IN ('original_value', 'simulated_value')",
                Integer.class));
        assertEquals(22, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public' AND table_name <> 'flyway_schema_history'",
                Integer.class));
        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'anomalies'",
                Integer.class));
        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'audit_events'",
                Integer.class));
        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pg_indexes WHERE schemaname = 'public' AND indexname = 'idx_anomalies_detected_at'",
                Integer.class));
        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public' AND table_name IN ('roles', 'permissions', 'role_permissions', 'user_roles', 'auth_bootstrap', 'alert_notifications', 'maintenance_plans', 'maintenance_tasks', 'work_orders')",
                Integer.class));
    }

    @Test
    void persistsUndoSnapshotsInTheirOwnTable() {
        CommandLogEntity command = commandLogRepository.saveAndFlush(CommandLogEntity.builder()
                .commandName("SIMULATE_FAILURE")
                .targetId("transformer-1")
                .executedBy("demo@example.com")
                .undoable(true)
                .build());
        commandSnapshotRepository.saveAndFlush(CommandSnapshotEntity.builder()
                .commandLog(command)
                .mementoState("{\"status\":\"NORMAL\"}")
                .build());

        var stored = commandSnapshotRepository
                .findFirstByCommandLog_ExecutedByAndConsumedAtIsNullOrderByCreatedAtDesc("demo@example.com");
        assertEquals("{\"status\":\"NORMAL\"}", stored.orElseThrow().getMementoState());
    }

    @Test
    void retentionPurgesOnlyTelemetryAndMlRecordsOlderThanConfiguredCutoff() {
        jdbcTemplate.update("""
                INSERT INTO sensor_readings (id, sensor_id, value, unit, timestamp, source)
                VALUES ('retention-reading-test', 'sens-001', 12.5, '°C', CURRENT_TIMESTAMP - INTERVAL '31 days', 'SIMULATION')
                """);
        jdbcTemplate.update("""
                INSERT INTO anomalies (id, transformer_id, anomaly_score, detected_at, strategy_used)
                VALUES ('retention-anomaly-test', 'trans-001', 0.9, CURRENT_TIMESTAMP - INTERVAL '31 days', 'TEST')
                """);
        jdbcTemplate.update("""
                INSERT INTO predictions (id, transformer_id, failure_probability, risk_level, evaluated_at, model_used)
                VALUES ('retention-prediction-test', 'trans-001', 0.9, 'HIGH', CURRENT_TIMESTAMP - INTERVAL '31 days', 'TEST')
                """);

        retentionService.purgeExpiredOperationalData();

        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sensor_readings WHERE id = 'retention-reading-test'", Integer.class));
        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM anomalies WHERE id = 'retention-anomaly-test'", Integer.class));
        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM predictions WHERE id = 'retention-prediction-test'", Integer.class));
    }
}
