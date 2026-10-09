package com.gridmind.security;

import com.gridmind.domain.UserRole;
import com.gridmind.dto.AuthUserDto;
import com.gridmind.dto.RegisterRequestDto;
import com.gridmind.entity.AuthBootstrapEntity;
import com.gridmind.entity.UserAccountEntity;
import com.gridmind.repository.AuthBootstrapRepository;
import com.gridmind.repository.UserAccountRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class AuthService {

    private final UserAccountRepository accountRepository;
    private final AuthBootstrapRepository bootstrapRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(
            UserAccountRepository accountRepository,
            AuthBootstrapRepository bootstrapRepository,
            PasswordEncoder passwordEncoder) {
        this.accountRepository = accountRepository;
        this.bootstrapRepository = bootstrapRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public AuthUserDto register(RegisterRequestDto request) {
        AuthBootstrapEntity bootstrap = bootstrapRepository.lockBootstrapRecord()
                .orElseThrow(() -> new IllegalStateException("Authentication bootstrap record is missing."));
        String email = request.email().trim().toLowerCase(Locale.ROOT);

        if (accountRepository.existsByEmail(email)) {
            throw new EmailAlreadyRegisteredException();
        }

        UserAccountEntity account = new UserAccountEntity();
        account.setDisplayName(request.displayName().trim());
        account.setEmail(email);
        account.setPasswordHash(passwordEncoder.encode(request.password()));
        account.setRole(!bootstrap.isBootstrapComplete() && accountRepository.count() == 0
                ? UserRole.ADMIN
                : UserRole.OPERATOR);

        try {
            accountRepository.saveAndFlush(account);
        } catch (DataIntegrityViolationException exception) {
            throw new EmailAlreadyRegisteredException();
        }

        if (!bootstrap.isBootstrapComplete()) {
            bootstrap.setBootstrapComplete(true);
        }
        return AuthUserDto.from(GridMindPrincipal.from(account));
    }
}
