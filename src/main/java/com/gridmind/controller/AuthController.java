package com.gridmind.controller;

import com.gridmind.dto.AuthUserDto;
import com.gridmind.dto.LoginRequestDto;
import com.gridmind.dto.RegisterRequestDto;
import com.gridmind.domain.UserRole;
import com.gridmind.security.AuthService;
import com.gridmind.security.EmailAlreadyRegisteredException;
import com.gridmind.security.GridMindPrincipal;
import com.gridmind.security.LoginAttemptLimiter;
import com.gridmind.security.RegistrationDisabledException;
import com.gridmind.security.TooManyLoginAttemptsException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final LoginAttemptLimiter loginAttemptLimiter;
    private final boolean demoEnabled;
    private final boolean requireHttps;
    private final String demoEmail;
    private final String demoPassword;

    public AuthController(
            AuthService authService,
            AuthenticationManager authenticationManager,
            SecurityContextRepository securityContextRepository,
            LoginAttemptLimiter loginAttemptLimiter,
            @Value("${auth.bootstrap.demo.enabled:true}") boolean demoEnabled,
            @Value("${auth.require-https:false}") boolean requireHttps,
            @Value("${auth.bootstrap.demo.email:}") String demoEmail,
            @Value("${auth.bootstrap.demo.password:}") String demoPassword) {
        this.authService = authService;
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.loginAttemptLimiter = loginAttemptLimiter;
        this.demoEnabled = demoEnabled;
        this.requireHttps = requireHttps;
        this.demoEmail = demoEmail.trim();
        this.demoPassword = demoPassword;
    }

    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken csrfToken) {
        return Map.of("token", csrfToken.getToken());
    }

    @PostMapping("/register")
    public ResponseEntity<AuthUserDto> register(@Valid @RequestBody RegisterRequestDto request,
                                                HttpServletRequest servletRequest) {
        checkSecureTransport(servletRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @Valid @RequestBody LoginRequestDto request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {
        return authenticate(request.email(), request.password(), servletRequest, servletResponse,
                request.administrator() ? UserRole.ADMIN : null);
    }

    @PostMapping("/demo-login")
    public ResponseEntity<?> demoLogin(
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {
        checkSecureTransport(servletRequest);
        if (!demoEnabled || demoEmail.isBlank() || demoPassword.isBlank()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("message", "La cuenta de demostración no está configurada."));
        }
        return authenticate(demoEmail, demoPassword, servletRequest, servletResponse, UserRole.DEMO);
    }

    private ResponseEntity<?> authenticate(
            String email,
            String password,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse,
            UserRole requiredRole) {
        checkSecureTransport(servletRequest);
        String remoteAddress = servletRequest.getRemoteAddr();
        try {
            loginAttemptLimiter.checkAllowed(remoteAddress);
        } catch (TooManyLoginAttemptsException exception) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .header(HttpHeaders.RETRY_AFTER, Long.toString(exception.getRetryAfterSeconds()))
                    .body(Map.of("message", exception.getMessage()));
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(
                            email.trim(),
                            password
                    )
            );
            if (!(authentication.getPrincipal() instanceof GridMindPrincipal principal)) {
                SecurityContextHolder.clearContext();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("message", "No se pudo completar el inicio de sesión."));
            }
            if (principal.role() == UserRole.DEMO && !demoEnabled) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "El acceso de demostración está desactivado."));
            }
            if (requiredRole == UserRole.ADMIN && principal.role() != UserRole.ADMIN) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "Esta cuenta no tiene permisos de administrador."));
            }
            if (requiredRole == UserRole.DEMO && principal.role() != UserRole.DEMO) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body(Map.of("message", "La cuenta de demostración no tiene el rol esperado."));
            }
            loginAttemptLimiter.recordSuccess(remoteAddress);
            if (servletRequest.getSession(false) != null) {
                servletRequest.changeSessionId();
            }
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            securityContextRepository.saveContext(context, servletRequest, servletResponse);
            return ResponseEntity.ok(AuthUserDto.from(principal));
        } catch (AuthenticationException exception) {
            loginAttemptLimiter.recordFailure(remoteAddress);
            SecurityContextHolder.clearContext();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Correo o contraseña incorrectos."));
        }
    }

    private void checkSecureTransport(HttpServletRequest request) {
        if (requireHttps && !request.isSecure()) {
            throw new HttpsRequiredException();
        }
    }

    @ExceptionHandler(HttpsRequiredException.class)
    public ResponseEntity<Map<String, String>> httpsRequired() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("message", "El acceso a GridMind requiere una conexión HTTPS."));
    }

    private static final class HttpsRequiredException extends RuntimeException {}

    @GetMapping("/me")
    public AuthUserDto currentUser(@AuthenticationPrincipal GridMindPrincipal principal) {
        return AuthUserDto.from(principal);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        SecurityContextHolder.clearContext();
        if (request.getSession(false) != null) {
            request.getSession(false).invalidate();
        }

        Cookie sessionCookie = new Cookie("JSESSIONID", "");
        sessionCookie.setPath("/");
        sessionCookie.setHttpOnly(true);
        sessionCookie.setSecure(requireHttps || request.isSecure());
        sessionCookie.setMaxAge(0);
        sessionCookie.setAttribute("SameSite", "Lax");
        response.addCookie(sessionCookie);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    public ResponseEntity<Map<String, String>> emailAlreadyRegistered(EmailAlreadyRegisteredException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", exception.getMessage()));
    }

    @ExceptionHandler(RegistrationDisabledException.class)
    public ResponseEntity<Map<String, String>> registrationDisabled(RegistrationDisabledException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", exception.getMessage()));
    }
}
