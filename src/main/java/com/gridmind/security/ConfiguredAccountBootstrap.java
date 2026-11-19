package com.gridmind.security;

import com.gridmind.domain.UserRole;
import com.gridmind.entity.UserAccountEntity;
import com.gridmind.repository.UserAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class ConfiguredAccountBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ConfiguredAccountBootstrap.class);
    private static final int MIN_PASSWORD_LENGTH = 12;

    private final UserAccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminPassword;
    private final boolean demoEnabled;
    private final String demoEmail;
    private final String demoPassword;

    public ConfiguredAccountBootstrap(
            UserAccountRepository accountRepository,
            PasswordEncoder passwordEncoder,
            @Value("${auth.bootstrap.admin.email:}") String adminEmail,
            @Value("${auth.bootstrap.admin.password:}") String adminPassword,
            @Value("${auth.bootstrap.demo.enabled:true}") boolean demoEnabled,
            @Value("${auth.bootstrap.demo.email:}") String demoEmail,
            @Value("${auth.bootstrap.demo.password:}") String demoPassword) {
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
        this.demoEnabled = demoEnabled;
        this.demoEmail = demoEmail;
        this.demoPassword = demoPassword;
    }

    @Override
    public void run(ApplicationArguments arguments) {
        bootstrapConfiguredAccount(adminEmail, adminPassword, "GridMind Administrator", UserRole.ADMIN);
        if (demoEnabled) {
            bootstrapConfiguredAccount(demoEmail, demoPassword, "GridMind Demo", UserRole.DEMO);
        }
    }

    private String normalizeDisplayName(String displayName, UserRole role) {
        String normalized = displayName == null ? null : displayName.trim();
        if (normalized == null || normalized.isBlank()) {
            normalized = role.name();
        }
        if (normalized.length() > 12) {
            normalized = normalized.substring(0, 12).trim();
        }
        return normalized.isBlank() ? role.name() : normalized;
    }

    private void bootstrapConfiguredAccount(String emailValue, String password, String displayName, UserRole role) {
        String email = emailValue == null ? "" : emailValue.trim().toLowerCase(Locale.ROOT);
        boolean hasEmail = !email.isBlank();
        boolean hasPassword = password != null && !password.isBlank();
        if (!hasEmail && !hasPassword) {
            return;
        }
        if (!hasEmail || !hasPassword || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$") || email.length() > 254
                || password.length() < MIN_PASSWORD_LENGTH || password.length() > 72) {
            throw new IllegalStateException("Configured " + role + " account requires a valid email and a password of 12 to 72 characters.");
        }

        UserAccountEntity existing = accountRepository.findByEmail(email).orElse(null);
        if (existing != null) {
            if (existing.getRole() != role) {
                throw new IllegalStateException("Configured " + role + " email is already assigned to a different role.");
            }
            log.info("Configured {} account already exists; preserving its stored credentials.", role);
            return;
        }

        UserAccountEntity account = new UserAccountEntity();
        account.setDisplayName(normalizeDisplayName(displayName, role));
        account.setEmail(email);
        account.setPasswordHash(passwordEncoder.encode(password));
        account.setRole(role);
        accountRepository.saveAndFlush(account);
        log.info("Created configured {} account.", role);
    }
}
