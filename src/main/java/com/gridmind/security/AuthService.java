package com.gridmind.security;

import com.gridmind.domain.UserRole;
import com.gridmind.dto.AuthUserDto;
import com.gridmind.dto.RegisterRequestDto;
import com.gridmind.entity.UserAccountEntity;
import com.gridmind.repository.UserAccountRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class AuthService {

    private final UserAccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final boolean registrationEnabled;

    public AuthService(
            UserAccountRepository accountRepository,
            PasswordEncoder passwordEncoder,
            @Value("${auth.registration.enabled:true}") boolean registrationEnabled) {
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
        this.registrationEnabled = registrationEnabled;
    }

    @Transactional
    public AuthUserDto register(RegisterRequestDto request) {
        if (!registrationEnabled) {
            throw new RegistrationDisabledException();
        }
        String email = request.email().trim().toLowerCase(Locale.ROOT);

        if (accountRepository.existsByEmail(email)) {
            throw new EmailAlreadyRegisteredException();
        }

        UserAccountEntity account = new UserAccountEntity();
        account.setDisplayName(request.displayName().trim());
        account.setEmail(email);
        account.setPasswordHash(passwordEncoder.encode(request.password()));
        account.setRole(UserRole.OPERATOR);

        try {
            accountRepository.saveAndFlush(account);
        } catch (DataIntegrityViolationException exception) {
            throw new EmailAlreadyRegisteredException();
        }

        return AuthUserDto.from(GridMindPrincipal.from(account));
    }
}
