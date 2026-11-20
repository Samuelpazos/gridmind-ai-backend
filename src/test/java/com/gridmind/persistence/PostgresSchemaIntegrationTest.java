package com.gridmind.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gridmind.domain.UserRole;
import com.gridmind.entity.UserAccountEntity;
import com.gridmind.repository.UserAccountRepository;
import com.gridmind.security.GridMindPrincipal;
import com.gridmind.simulation.SensorSimulator;
import org.flywaydb.core.Flyway;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
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

@AutoConfigureMockMvc
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
    private MockMvc mvc;
    @Autowired
    private SensorSimulator sensorSimulator;
    @Autowired
    private UserAccountRepository accounts;
    @Autowired
    private ObjectMapper objectMapper;

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
    void operatorsOnlySeeAndChangeRecordsInTheirOwnWorkspace() throws Exception {
        var first = operator("scope-a@example.test");
        var second = operator("scope-b@example.test");

        mvc.perform(get("/api/grid").with(authentication(first)))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
        mvc.perform(get("/api/grid").with(authentication(second)))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());

        String firstAsset = addTestAsset(first, "Equipo de Paula");
        String secondAsset = addTestAsset(second, "Equipo de Julián");

        mvc.perform(get("/api/grid").with(authentication(first))).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].transformers[0].id").value(firstAsset));
        mvc.perform(get("/api/grid").with(authentication(second))).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].transformers[0].id").value(secondAsset));
        mvc.perform(delete("/api/grid/transformers/{id}", firstAsset).with(authentication(second)).with(csrf()))
                .andExpect(status().isNotFound());

        String ownerSensorCode = jdbcTemplate.queryForObject(
                "SELECT code FROM sensors WHERE transformer_id = ?", String.class, firstAsset);
        String secondReal = addAsset(second, "Equipo real privado", "HARDWARE", "[]");
        mvc.perform(patch("/api/grid/transformers/{id}/temperature-sensor", secondReal)
                        .with(authentication(second)).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of("sensorCode", ownerSensorCode))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Este código de sensor ya está en uso. Usa un código distinto para otra placa."));

        String sensorId = jdbcTemplate.queryForObject(
                "SELECT id FROM sensors WHERE transformer_id = ?", String.class, firstAsset);
        jdbcTemplate.update("""
                INSERT INTO sensor_readings (id, sensor_id, value, unit, timestamp, source)
                VALUES ('workspace-reading', ?, 32.5, '°C', CURRENT_TIMESTAMP, 'SIMULATION')
                """, sensorId);
        jdbcTemplate.update("""
                INSERT INTO alerts (id, transformer_id, severity, message)
                VALUES ('workspace-alert', ?, 'WARNING', 'Alerta privada')
                """, firstAsset);
        mvc.perform(get("/api/components/{id}/readings", sensorId).with(authentication(first)))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].value").value(32.5));
        mvc.perform(get("/api/components/{id}/readings", sensorId).with(authentication(second)))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
        mvc.perform(get("/api/alerts").with(authentication(first)))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].message").value("Alerta privada"));
        mvc.perform(post("/api/alerts/workspace-alert/ack").with(authentication(second)).with(csrf()))
                .andExpect(status().isBadRequest());
        assertEquals(false, jdbcTemplate.queryForObject(
                "SELECT acknowledged FROM alerts WHERE id = 'workspace-alert'", Boolean.class));
        mvc.perform(get("/api/alerts").with(authentication(second)))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());

        mvc.perform(post("/api/simulations").with(authentication(first)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"transformerId":"%s","variableModified":"TEMPERATURE","targetValue":96}
                                """.formatted(firstAsset)))
                .andExpect(status().isOk());
        mvc.perform(get("/api/simulations").with(authentication(first)))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].transformerId").value(firstAsset));
        mvc.perform(get("/api/simulations").with(authentication(second)))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());

        mvc.perform(put("/api/config/strategy").with(authentication(first)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"strategyName":"THRESHOLD_STRATEGY"}
                                """))
                .andExpect(status().isOk()).andExpect(jsonPath("$.strategyName").value("THRESHOLD_STRATEGY"));
        mvc.perform(get("/health").with(authentication(second)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.activeStrategy").value("SUPERVISED_MODEL_STRATEGY"));

        mvc.perform(delete("/api/grid/transformers/{id}", firstAsset).with(authentication(first)).with(csrf()))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/grid/transformers/archived").with(authentication(first)))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(firstAsset));
        mvc.perform(get("/api/grid/transformers/archived").with(authentication(second)))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void manualTelemetryCreatesRealAlertsOnlyForTheSelectedSimulatedAsset() throws Exception {
        var operator = operator("manual-scenario@example.test");
        var other = operator("manual-other@example.test");
        String selected = addTestAsset(operator, "Equipo seleccionado");
        String untouched = addTestAsset(operator, "Equipo sin escenario");
        String physical = addAsset(operator, "Equipo físico", "HARDWARE", "[]");
        String request = """
                {"transformerId":"%s","values":{"TEMPERATURE":100}}
                """;
        mvc.perform(put("/api/config/simulator/manual").with(authentication(other)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(request.formatted(selected)))
                .andExpect(status().isNotFound());
        mvc.perform(put("/api/config/simulator/manual").with(authentication(operator)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(request.formatted(physical)))
                .andExpect(status().isNotFound());
        mvc.perform(put("/api/config/simulator/manual").with(authentication(operator)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"transformerId":"%s","values":{"CURRENT":500}}
                                """.formatted(selected)))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/config/simulator/manual").with(authentication(operator)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(request.formatted(selected)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.values.TEMPERATURE").value(100));

        try {
            sensorSimulator.generateSensorTelemetryTick();
            assertEquals(1, jdbcTemplate.queryForObject("""
                    SELECT count(*) FROM alerts
                    WHERE transformer_id = ? AND severity = 'CRITICAL' AND source = 'SENSOR_OBSERVER'
                    """, Integer.class, selected));
            String alertId = jdbcTemplate.queryForObject("""
                    SELECT id FROM alerts WHERE transformer_id = ? AND source = 'SENSOR_OBSERVER'
                    """, String.class, selected);
            mvc.perform(get("/api/alerts").with(authentication(operator)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].readingSource").value("SIMULATION"))
                    .andExpect(jsonPath("$[0].readingTimestamp").isString());
            mvc.perform(post("/api/alerts/{id}/assistant", alertId).with(authentication(operator)).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON).content("{\"topic\":\"event\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.provider").value("Datos de GridMind"))
                                        .andExpect(jsonPath("$.triggeringReading.value").value(100))
                    .andExpect(jsonPath("$.triggeringReading.source").value("SIMULATION"));
            mvc.perform(post("/api/alerts/{id}/assistant", alertId).with(authentication(other)).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON).content("{\"topic\":\"event\"}"))
                    .andExpect(status().isNotFound());
            assertEquals(0, jdbcTemplate.queryForObject("""
                    SELECT count(*) FROM sensor_readings r JOIN sensors s ON r.sensor_id = s.id
                    WHERE s.transformer_id = ?
                    """, Integer.class, untouched));
            mvc.perform(get("/api/alerts").with(authentication(other)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
        } finally {
            mvc.perform(delete("/api/config/simulator/manual").with(authentication(operator)).with(csrf()))
                    .andExpect(status().isNoContent());
        }
        mvc.perform(get("/api/config/simulator").with(authentication(operator)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.active").value(false))
                .andExpect(jsonPath("$.manualScenario").doesNotExist());
    }

    @Test
    void failureCommandCreatesOnePrivateCriticalAlertAndRejectsPhysicalAssets() throws Exception {
        var operator = operator("failure-command@example.test");
        var other = operator("failure-command-other@example.test");
        String simulated = addTestAsset(operator, "Falla de prueba");
        String physical = addAsset(operator, "Equipo real", "HARDWARE", "[]");
        String command = """
                {"commandName":"SIMULATE_FAILURE","targetId":"%s"}
                """;

        mvc.perform(post("/api/commands/execute").with(authentication(operator)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(command.formatted(physical)))
                .andExpect(status().isBadRequest());
        for (int attempt = 0; attempt < 2; attempt++) {
            mvc.perform(post("/api/commands/execute").with(authentication(operator)).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON).content(command.formatted(simulated)))
                    .andExpect(status().isOk());
        }
        mvc.perform(get("/api/alerts").with(authentication(operator)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].severity").value("CRITICAL"))
                .andExpect(jsonPath("$[0].assetType").value("TEST"))
                .andExpect(jsonPath("$[0].source").value("MANUAL_FAILURE_SIMULATION"));
        mvc.perform(get("/api/alerts").with(authentication(other)))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
    }


    @Test
    void maintenanceIsPrivateAndDuplicateSchedulingDoesNotReplaceUndoSnapshot() throws Exception {
        var owner = operator("maintenance-owner@example.test");
        var other = operator("maintenance-other@example.test");
        String asset = addTestAsset(owner, "Equipo privado");
        String command = """
                {"commandName":"SCHEDULE_MAINTENANCE","targetId":"%s"}
                """.formatted(asset);

        mvc.perform(post("/api/commands/execute").with(authentication(owner)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(command))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maintenance.transformerCode").isString())
                .andExpect(jsonPath("$.maintenance.scheduledDate").isString());
        mvc.perform(post("/api/commands/execute").with(authentication(owner)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(command))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/commands/execute").with(authentication(other)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(command))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/maintenances").with(authentication(other)))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
        mvc.perform(get("/api/maintenances").with(authentication(owner)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));

        String maintenanceId = jdbcTemplate.queryForObject(
                "SELECT id FROM maintenance_recommendations WHERE transformer_id = ?", String.class, asset);
        mvc.perform(post("/api/maintenances/{id}/complete", maintenanceId).with(authentication(other)).with(csrf()))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/commands/undo").with(authentication(owner)).with(csrf()))
                .andExpect(status().isOk());
        mvc.perform(get("/api/maintenances").with(authentication(owner)))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void scenarioExposesReferenceInputsAndRejectsOtherWorkspaces() throws Exception {
        var owner = operator("scenario-owner@example.test");
        var other = operator("scenario-other@example.test");
        String asset = addAsset(owner, "Escenario privado", "TEST", "[\"TEMPERATURE\",\"VOLTAGE\"]");
        String request = """
                {"transformerId":"%s","variableModified":"TEMPERATURE","targetValue":100}
                """.formatted(asset);
        mvc.perform(post("/api/simulations").with(authentication(other)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/simulations").with(authentication(owner)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.criticalAlertThreshold").value(88))
                .andExpect(jsonPath("$.warningAlertThreshold").value(76))
                .andExpect(jsonPath("$.referenceInputs[0]").value("VOLTAGE"));
    }

    private UsernamePasswordAuthenticationToken operator(String email) {
        var account = new UserAccountEntity();
        account.setEmail(email);
        account.setDisplayName("Operador");
        account.setPasswordHash("unused-in-this-test");
        account.setRole(UserRole.OPERATOR);
        var principal = GridMindPrincipal.from(accounts.saveAndFlush(account));
        return UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities());
    }

    private String addTestAsset(UsernamePasswordAuthenticationToken auth, String name) throws Exception {
        return addAsset(auth, name, "TEST", "[\"TEMPERATURE\"]");
    }

    private String addAsset(UsernamePasswordAuthenticationToken auth, String name, String type, String sensors)
            throws Exception {
        String response = mvc.perform(post("/api/grid/transformers").with(authentication(auth)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {
                                  "substationType":"URBAN",
                                  "substationName":"Subestación privada",
                                  "substationLocation":"Laboratorio",
                                  "transformerName":"%s",
                                  "assetType":"%s",
                                  "ratingKva":750,
                                  "ageYears":4,
                                  "sensorTypes":%s
                                }
                                """.formatted(name, type, sensors)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asText();
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
