package com.gridmind;

import com.gridmind.config.SecurityConfig;
import com.gridmind.controller.ConfigController;
import com.gridmind.controller.DemoScenarioController;
import com.gridmind.controller.GridProvisioningController;
import com.gridmind.controller.HardwareTelemetryController;
import com.gridmind.controller.CommandController;
import com.gridmind.controller.MaintenanceController;
import com.gridmind.controller.SimulationController;
import com.gridmind.controller.OperationalCatalogController;
import com.gridmind.domain.UserRole;
import com.gridmind.dto.AuthUserDto;
import com.gridmind.dto.SensorReadingDto;
import com.gridmind.dto.TelemetryStatusDto;
import com.gridmind.service.AiConfigService;
import com.gridmind.service.CommandService;
import com.gridmind.security.AuthService;
import com.gridmind.security.GridMindPrincipal;
import com.gridmind.security.GridMindUserDetailsService;
import com.gridmind.security.LoginAttemptLimiter;
import com.gridmind.security.TooManyLoginAttemptsException;
import com.gridmind.service.GridProvisioningService;
import com.gridmind.service.HardwareTelemetryService;
import com.gridmind.service.MaintenanceService;
import com.gridmind.service.OperationalCatalogService;
import com.gridmind.service.SimulationService;
import com.gridmind.simulation.SensorSimulator;
import com.gridmind.simulation.SimulationScenario;
import com.gridmind.ai.SmileMlService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        com.gridmind.controller.AuthController.class,
        HardwareTelemetryController.class,
        DemoScenarioController.class,
        ConfigController.class,
        GridProvisioningController.class,
        SimulationController.class,
        CommandController.class,
        MaintenanceController.class,
        OperationalCatalogController.class
}, properties = {
        "auth.bootstrap.demo.email=demo@example.com",
        "auth.bootstrap.demo.password=configured-demo-password"
})
@Import(SecurityConfig.class)
class AuthSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;
    @MockitoBean
    private AuthenticationManager authenticationManager;
    @MockitoBean
    private GridMindUserDetailsService userDetailsService;
    @MockitoBean
    private LoginAttemptLimiter loginAttemptLimiter;
    @MockitoBean
    private HardwareTelemetryService telemetryService;
    @MockitoBean
    private SmileMlService smileMlService;
    @MockitoBean
    private AiConfigService aiConfigService;
    @MockitoBean
    private SensorSimulator sensorSimulator;
    @MockitoBean
    private GridProvisioningService gridProvisioningService;
    @MockitoBean
    private SimulationService simulationService;
    @MockitoBean
    private CommandService commandService;
    @MockitoBean
    private MaintenanceService maintenanceService;
    @MockitoBean
    private OperationalCatalogService operationalCatalogService;

    @Test
    void protectsApplicationApiWhenNoSessionExists() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/telemetry/status"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(put("/api/config/strategy")
                        .with(csrf())
                        .with(user("operator").roles("OPERATOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void registrationRequiresCsrfProtection() throws Exception {
        String request = """
                {"displayName":"Operator","email":"operator@example.com","password":"long-password-123"}
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isForbidden());

        when(authService.register(any())).thenReturn(
                new AuthUserDto("user-1", "Operator", "operator@example.com", "OPERATOR"));
        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("OPERATOR"));
    }

    @Test
    void reportsInvalidRegistrationFieldsAsBadRequest() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"displayName":"Nombre demasiado largo","email":"operator@example.com","password":"long-password-123"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.details.displayName").exists());
    }

    @Test
    void demoAccountConfigIsPublicAndDoesNotExposeCredentials() throws Exception {
        mockMvc.perform(get("/api/demo/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.email").value("demo@example.com"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void demoCanSimulateChangeStrategyChangeScenarioRunCommandsAndCompleteMaintenance() throws Exception {
        when(sensorSimulator.isActive()).thenReturn(true);
        when(sensorSimulator.getCurrentScenario()).thenReturn(SimulationScenario.SUDDEN_FAILURE);

        mockMvc.perform(post("/api/simulations")
                        .with(csrf())
                        .with(user("demo").roles("DEMO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"transformerId":"transformer-1","variableModified":"TEMPERATURE","deltaPercent":25}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/config/strategy")
                        .with(csrf())
                        .with(user("demo").roles("DEMO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"strategyName":"THRESHOLD_STRATEGY","forceDegradedMode":true}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/config/simulator")
                        .with(csrf())
                        .with(user("demo").roles("DEMO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"active":true,"scenario":"SUDDEN_FAILURE"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/commands/execute")
                        .with(csrf())
                        .with(user("demo").roles("DEMO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"commandName":"SIMULATE_FAILURE","targetId":"transformer-1","parameters":{}}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/maintenances/maintenance-1/complete")
                        .with(csrf())
                        .with(user("demo").roles("DEMO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"resultNotes":"Mantenimiento de demostración"}
                                """))
                .andExpect(status().isOk());

        org.mockito.Mockito.verify(simulationService).executeSimulation(any());
        org.mockito.Mockito.verify(aiConfigService).setActiveStrategyName("THRESHOLD_STRATEGY");
        org.mockito.Mockito.verify(sensorSimulator).setCurrentScenario(SimulationScenario.SUDDEN_FAILURE);
        org.mockito.Mockito.verify(commandService).executeCommand(any(), org.mockito.ArgumentMatchers.eq("demo"));
        org.mockito.Mockito.verify(maintenanceService).completeMaintenance(
                "maintenance-1", "Mantenimiento de demostración");
    }

    @Test
    void demoCannotManageUsersCreateSubstationsOrEditCatalogs() throws Exception {
        mockMvc.perform(post("/api/grid/substations")
                        .with(csrf())
                        .with(user("demo").roles("DEMO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"URBAN","name":"Demo","location":"Demo","transformerName":"Demo",
                                 "ratingKva":500,"ageYears":1,"sensorTypes":["TEMPERATURE"]}
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/grid/transformers")
                        .with(csrf())
                        .with(user("demo").roles("DEMO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/users")
                        .with(csrf())
                        .with(user("demo").roles("DEMO")))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/catalogs/thresholds/sensor-1")
                        .with(csrf())
                        .with(user("demo").roles("DEMO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/catalogs/overview")
                        .with(user("demo").roles("DEMO")))
                .andExpect(status().isForbidden());
    }

    @Test
    void demoCannotEditArchiveRestoreOrBrowseArchivedTransformers() throws Exception {
        mockMvc.perform(get("/api/grid/transformers/archived")
                        .with(user("demo").roles("DEMO")))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/grid/transformers/transformer-1/name")
                        .with(csrf())
                        .with(user("demo").roles("DEMO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Renombrado\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/grid/transformers/transformer-1")
                        .with(csrf())
                        .with(user("demo").roles("DEMO")))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/grid/transformers/transformer-1/restore")
                        .with(csrf())
                        .with(user("demo").roles("DEMO")))
                .andExpect(status().isForbidden());
    }

    @Test
    void corsAllowsPatchRequestsForTransformerRenaming() throws Exception {
        mockMvc.perform(options("/api/grid/transformers/transformer-1/name")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "PATCH")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "content-type,x-xsrf-token"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                        .string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS,
                                org.hamcrest.Matchers.containsString("PATCH")));
    }

    @Test
    void adminCanReviewOperationalCatalogInventory() throws Exception {
        when(operationalCatalogService.getCatalogInventory())
                .thenReturn(java.util.Map.of("sensorThresholds", 2L));

        mockMvc.perform(get("/api/catalogs/overview")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sensorThresholds").value(2));
    }

    @Test
    void adminCanCreateSubstationThroughTheGridProvisioningRoute() throws Exception {
        mockMvc.perform(post("/api/grid/substations")
                        .with(csrf())
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"URBAN","name":"North","location":"North zone",
                                 "transformerName":"T-N-001","ratingKva":500,
                                 "ageYears":1,"sensorTypes":["TEMPERATURE"]}
                                """))
                .andExpect(status().isCreated());

        org.mockito.Mockito.verify(gridProvisioningService).provision(any());
    }

    @Test
    void adminCanCreateTransformerThroughTheManualProvisioningRoute() throws Exception {
        when(gridProvisioningService.provisionTransformer(any()))
                .thenReturn(com.gridmind.dto.TransformerDto.builder().build());

        mockMvc.perform(post("/api/grid/transformers")
                        .with(csrf())
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"substationId":"sub-001","transformerName":"Hardware Norte 2",
                                 "transformerCode":"T-N-002","assetType":"HARDWARE",
                                 "ratingKva":750,"ageYears":4,
                                 "sensorTypes":["TEMPERATURE"],
                                 "sensorCodes":{"TEMPERATURE":"SN-002-TMP"}}
                                """))
                .andExpect(status().isCreated());

        org.mockito.Mockito.verify(gridProvisioningService).provisionTransformer(any());
    }

    @Test
    void csrfTokenEndpointIsAvailableBeforeLogin() throws Exception {
        mockMvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void deviceTelemetryEndpointUsesItsDeviceTokenInsteadOfBrowserSessionOrCsrf() throws Exception {
        SensorReadingDto savedReading = SensorReadingDto.builder()
                .id("reading-1")
                .sensorCode("SN-001-TMP")
                .value(32.5)
                .unit("°C")
                .source("HARDWARE")
                .build();
        when(telemetryService.ingestBatch(any())).thenReturn(java.util.List.of(savedReading));

        mockMvc.perform(post("/api/telemetry/readings")
                        .header("X-Device-Token", "device-token-example")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                [{"sensorCode":"SN-001-TMP","value":32.5,"unit":"°C"}]
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].source").value("HARDWARE"));
        org.mockito.Mockito.verify(telemetryService).authenticateDevice("device-token-example");
    }

    @Test
    void telemetryStatusEndpointReturnsTheRecentReadingSource() throws Exception {
        when(telemetryService.getStatus()).thenReturn(new TelemetryStatusDto(
                true,
                false,
                "Modelo entrenado con datos sintéticos.",
                "SIMULATION",
                java.time.ZonedDateTime.parse("2026-03-16T12:00:00Z")
        ));

        mockMvc.perform(get("/api/telemetry/status")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recentDataSource").value("SIMULATION"))
                .andExpect(jsonPath("$.simulatorActive").value(false))
                .andExpect(jsonPath("$.lastHardwareReadingAt").exists());
    }

    @Test
    void loginCreatesAServerSessionThatAuthenticatesSubsequentRequests() throws Exception {
        GridMindPrincipal principal = new GridMindPrincipal(
                "user-1",
                "Grid Administrator",
                "admin@example.com",
                "encoded-password",
                UserRole.ADMIN
        );
        when(authenticationManager.authenticate(any())).thenReturn(
                UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities()));

        var loginResult = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"admin@example.com","password":"long-password-123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("admin@example.com"));
    }

    @Test
    void demoLoginRequiresCsrfProtection() throws Exception {
        mockMvc.perform(post("/api/auth/demo-login"))
                .andExpect(status().isForbidden());
    }

    @Test
    void demoLoginCreatesOnlyADemoSessionUsingServerSideCredentials() throws Exception {
        GridMindPrincipal principal = new GridMindPrincipal(
                "demo-1",
                "GridMind Demo",
                "demo@example.com",
                "encoded-password",
                UserRole.DEMO
        );
        when(authenticationManager.authenticate(any())).thenReturn(
                UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities()));

        var loginResult = mockMvc.perform(post("/api/auth/demo-login")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("DEMO"))
                .andReturn();

        var authenticationCaptor = org.mockito.ArgumentCaptor.forClass(
                org.springframework.security.core.Authentication.class);
        org.mockito.Mockito.verify(authenticationManager).authenticate(authenticationCaptor.capture());
        org.springframework.security.core.Authentication credentials = authenticationCaptor.getValue();
        org.junit.jupiter.api.Assertions.assertEquals("demo@example.com", credentials.getName());
        org.junit.jupiter.api.Assertions.assertEquals("configured-demo-password", credentials.getCredentials());

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("DEMO"));
    }

    @Test
    void demoLoginRejectsAnAccountWithAnotherRole() throws Exception {
        GridMindPrincipal principal = new GridMindPrincipal(
                "admin-1",
                "Grid Administrator",
                "demo@example.com",
                "encoded-password",
                UserRole.ADMIN
        );
        when(authenticationManager.authenticate(any())).thenReturn(
                UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities()));

        mockMvc.perform(post("/api/auth/demo-login")
                        .with(csrf()))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value("La cuenta de demostración no tiene el rol esperado."));
    }

    @Test
    void throttlesRepeatedLoginAttempts() throws Exception {
        doThrow(new TooManyLoginAttemptsException(120))
                .when(loginAttemptLimiter).checkAllowed("127.0.0.1");

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"admin@example.com","password":"long-password-123"}
                                """))
                .andExpect(status().isTooManyRequests())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                        .string(HttpHeaders.RETRY_AFTER, "120"));
    }
}
