package com.gridmind;

import com.gridmind.controller.AuthController;
import com.gridmind.security.AuthService;
import com.gridmind.security.LoginAttemptLimiter;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthTransportTest {

    private static final String LOGIN = """
            {"email":"admin@example.test","password":"example-password","administrator":true}
            """;

    private final AuthService authService = mock(AuthService.class);
    private final AuthenticationManager authentication = mock(AuthenticationManager.class);
    private final LoginAttemptLimiter limiter = mock(LoginAttemptLimiter.class);

    @Test
    void productionRejectsPasswordOperationsOverHttp() throws Exception {
        var mvc = MockMvcBuilders.standaloneSetup(controller(true)).build();
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(LOGIN))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"displayName":"Prueba","email":"user@example.test","password":"example-password"}
                                """))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/auth/demo-login")).andExpect(status().isForbidden());
        verifyNoInteractions(authService, authentication, limiter);
    }

    @Test
    void productionAllowsAuthenticationToProceedOverHttps() throws Exception {
        when(authentication.authenticate(any())).thenThrow(new BadCredentialsException("Invalid credentials"));
        var mvc = MockMvcBuilders.standaloneSetup(controller(true)).build();
        mvc.perform(post("/api/auth/login").secure(true).contentType(MediaType.APPLICATION_JSON).content(LOGIN))
                .andExpect(status().isUnauthorized());
        verify(authentication).authenticate(any());
    }

    private AuthController controller(boolean requireHttps) {
        return new AuthController(authService, authentication, new HttpSessionSecurityContextRepository(),
                limiter, true, requireHttps, "demo@example.test", "example-password");
    }
}
