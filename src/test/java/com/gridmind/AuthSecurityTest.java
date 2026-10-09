package com.gridmind;

import com.gridmind.config.SecurityConfig;
import com.gridmind.controller.HardwareTelemetryController;
import com.gridmind.domain.UserRole;
import com.gridmind.dto.AuthUserDto;
import com.gridmind.dto.SensorReadingDto;
import com.gridmind.security.AuthService;
import com.gridmind.security.GridMindPrincipal;
import com.gridmind.security.GridMindUserDetailsService;
import com.gridmind.service.HardwareTelemetryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        com.gridmind.controller.AuthController.class,
        HardwareTelemetryController.class
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
    private HardwareTelemetryService telemetryService;

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
                {"displayName":"First Admin","email":"admin@example.com","password":"long-password-123"}
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isForbidden());

        when(authService.register(any())).thenReturn(
                new AuthUserDto("user-1", "First Admin", "admin@example.com", "ADMIN"));
        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("ADMIN"));
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
}
