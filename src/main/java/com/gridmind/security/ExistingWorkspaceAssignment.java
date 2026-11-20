package com.gridmind.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(1)
class ExistingWorkspaceAssignment implements ApplicationRunner {

    private final JdbcTemplate jdbc;
    private final String ownerEmail;

    ExistingWorkspaceAssignment(JdbcTemplate jdbc, @Value("${auth.bootstrap.admin.email:}") String ownerEmail) {
        this.jdbc = jdbc;
        this.ownerEmail = ownerEmail.trim();
    }

    @Override
    public void run(ApplicationArguments arguments) {
        if (!ownerEmail.isEmpty()) {
            jdbc.update("""
                    UPDATE substations s SET owner_account_id = u.id
                    FROM user_accounts u
                    WHERE s.owner_account_id IS NULL AND lower(u.email) = lower(?) AND u.role = 'ADMIN'
                    """, ownerEmail);
        }
    }
}

