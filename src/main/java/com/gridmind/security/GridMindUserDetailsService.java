package com.gridmind.security;

import com.gridmind.repository.UserAccountRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class GridMindUserDetailsService implements UserDetailsService {

    private final UserAccountRepository accountRepository;

    public GridMindUserDetailsService(UserAccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String normalizedEmail = username.trim().toLowerCase(Locale.ROOT);
        return accountRepository.findByEmail(normalizedEmail)
                .map(GridMindPrincipal::from)
                .orElseThrow(() -> new UsernameNotFoundException("Invalid email or password."));
    }
}
