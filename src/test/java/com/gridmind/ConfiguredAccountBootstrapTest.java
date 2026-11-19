package com.gridmind;

import com.gridmind.domain.UserRole;
import com.gridmind.entity.UserAccountEntity;
import com.gridmind.repository.UserAccountRepository;
import com.gridmind.security.ConfiguredAccountBootstrap;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ConfiguredAccountBootstrapTest {

    @Test
    void createsConfiguredAdminAndDemoAccountsWithHashedPasswords() {
        UserAccountRepository repository = mock(UserAccountRepository.class);
        var passwordEncoder = new BCryptPasswordEncoder();
        when(repository.findByEmail("admin@example.com")).thenReturn(Optional.empty());
        when(repository.findByEmail("demo@example.com")).thenReturn(Optional.empty());
        when(repository.saveAndFlush(any(UserAccountEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        var bootstrap = new ConfiguredAccountBootstrap(
                repository,
                passwordEncoder,
                "ADMIN@example.com",
                "a-long-admin-password",
                true,
                "demo@example.com",
                "a-long-demo-password"
        );

        bootstrap.run(null);

        var captor = org.mockito.ArgumentCaptor.forClass(UserAccountEntity.class);
        verify(repository, times(2)).saveAndFlush(captor.capture());
        assertEquals(UserRole.ADMIN, captor.getAllValues().getFirst().getRole());
        assertEquals(UserRole.DEMO, captor.getAllValues().getLast().getRole());
        assertTrue(passwordEncoder.matches("a-long-admin-password",
                captor.getAllValues().getFirst().getPasswordHash()));
        assertTrue(passwordEncoder.matches("a-long-demo-password",
                captor.getAllValues().getLast().getPasswordHash()));
    }

    @Test
    void doesNotCreateAccountsWhenNoBootstrapCredentialsAreConfigured() {
        UserAccountRepository repository = mock(UserAccountRepository.class);
        var bootstrap = new ConfiguredAccountBootstrap(
                repository, new BCryptPasswordEncoder(), "", "", true, "", "");

        bootstrap.run(null);

        verifyNoInteractions(repository);
    }

    @Test
    void rejectsPartiallyConfiguredOrWeakBootstrapCredentials() {
        UserAccountRepository repository = mock(UserAccountRepository.class);
        var bootstrap = new ConfiguredAccountBootstrap(
                repository, new BCryptPasswordEncoder(), "admin@example.com", "short", true, "", "");

        assertThrows(IllegalStateException.class, () -> bootstrap.run(null));
        verifyNoInteractions(repository);
    }

    @Test
    void demoModeCanDisableDemoAccountBootstrap() {
        UserAccountRepository repository = mock(UserAccountRepository.class);
        when(repository.findByEmail("admin@example.com")).thenReturn(Optional.empty());
        when(repository.saveAndFlush(any(UserAccountEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        var bootstrap = new ConfiguredAccountBootstrap(
                repository,
                new BCryptPasswordEncoder(),
                "admin@example.com",
                "a-long-admin-password",
                false,
                "demo@example.com",
                "a-long-demo-password"
        );

        bootstrap.run(null);

        var accountCaptor = org.mockito.ArgumentCaptor.forClass(UserAccountEntity.class);
        verify(repository).saveAndFlush(accountCaptor.capture());
        assertEquals(UserRole.ADMIN, accountCaptor.getValue().getRole());
        verify(repository, never()).findByEmail("demo@example.com");
    }
}
