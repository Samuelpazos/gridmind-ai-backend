package com.gridmind;

import com.gridmind.domain.UserRole;
import com.gridmind.dto.RegisterRequestDto;
import com.gridmind.entity.AuthBootstrapEntity;
import com.gridmind.entity.UserAccountEntity;
import com.gridmind.repository.AuthBootstrapRepository;
import com.gridmind.repository.UserAccountRepository;
import com.gridmind.security.AuthService;
import com.gridmind.security.EmailAlreadyRegisteredException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserAccountRepository accountRepository;
    @Mock
    private AuthBootstrapRepository bootstrapRepository;

    private PasswordEncoder passwordEncoder;
    private AuthService authService;
    private AuthBootstrapEntity bootstrap;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        authService = new AuthService(accountRepository, bootstrapRepository, passwordEncoder);
        bootstrap = new AuthBootstrapEntity();
        bootstrap.setId(1);
        when(bootstrapRepository.lockBootstrapRecord()).thenReturn(Optional.of(bootstrap));
    }

    @Test
    void assignsAdministratorRoleToFirstAccountAndHashesPassword() {
        when(accountRepository.existsByEmail("first@example.com")).thenReturn(false);
        when(accountRepository.count()).thenReturn(0L);
        when(accountRepository.saveAndFlush(any(UserAccountEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var user = authService.register(new RegisterRequestDto("First Operator", " First@Example.com ", "long-password-123"));

        ArgumentCaptor<UserAccountEntity> accountCaptor = ArgumentCaptor.forClass(UserAccountEntity.class);
        verify(accountRepository).saveAndFlush(accountCaptor.capture());
        UserAccountEntity saved = accountCaptor.getValue();
        assertEquals(UserRole.ADMIN.name(), user.role());
        assertEquals("first@example.com", user.email());
        assertNotEquals("long-password-123", saved.getPasswordHash());
        assertTrue(passwordEncoder.matches("long-password-123", saved.getPasswordHash()));
        assertEquals("first@example.com", saved.getEmail());
        assertTrue(bootstrap.isBootstrapComplete());
    }

    @Test
    void assignsOperatorRoleAfterBootstrapAccountExists() {
        bootstrap.setBootstrapComplete(true);
        when(accountRepository.existsByEmail("operator@example.com")).thenReturn(false);
        when(accountRepository.saveAndFlush(any(UserAccountEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var user = authService.register(new RegisterRequestDto("Grid Operator", "operator@example.com", "long-password-123"));

        assertEquals(UserRole.OPERATOR.name(), user.role());
    }

    @Test
    void rejectsEmailThatIsAlreadyRegistered() {
        when(accountRepository.existsByEmail("first@example.com")).thenReturn(true);

        assertThrows(EmailAlreadyRegisteredException.class,
                () -> authService.register(new RegisterRequestDto("First Operator", "first@example.com", "long-password-123")));
        verify(accountRepository, never()).saveAndFlush(any());
    }
}
