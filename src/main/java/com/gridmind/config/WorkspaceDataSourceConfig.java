package com.gridmind.config;

import com.gridmind.security.GridMindPrincipal;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DelegatingDataSource;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@Configuration(proxyBeanMethods = false)
public class WorkspaceDataSourceConfig {

    public static @Nullable GridMindPrincipal currentAccount() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof GridMindPrincipal principal ? principal : null;
    }

    @Bean
    static BeanPostProcessor workspaceConnectionScope() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) {
                return "dataSource".equals(beanName) && bean instanceof DataSource dataSource
                        ? new WorkspaceDataSource(dataSource) : bean;
            }
        };
    }

    private static final class WorkspaceDataSource extends DelegatingDataSource {
        WorkspaceDataSource(DataSource target) {
            super(target);
        }

        @Override
        public Connection getConnection() throws SQLException {
            return prepare(super.getConnection());
        }

        @Override
        public Connection getConnection(String username, String password) throws SQLException {
            return prepare(super.getConnection(username, password));
        }

        private static boolean isSystemEndpoint() {
            if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
                return false;
            }
            return switch (attributes.getRequest().getServletPath()) {
                case "/api/auth/login", "/api/auth/register", "/api/auth/demo-login", "/api/telemetry/readings" -> true;
                default -> false;
            };
        }

        private Connection prepare(Connection connection) throws SQLException {
            try {
                // A pooled connection can have belonged to another browser request.
                try (var statement = connection.createStatement()) {
                    statement.execute("RESET ROLE");
                }
                var account = isSystemEndpoint() ? null : currentAccount();
                try (var statement = connection.prepareStatement("""
                        SELECT set_config('gridmind.account_id', ?, false),
                               set_config('gridmind.account_email', ?, false)
                        """)) {
                    statement.setString(1, account == null ? "" : account.id());
                    statement.setString(2, account == null ? "" : account.email());
                    statement.execute();
                }
                if (account != null) {
                    try (var statement = connection.createStatement()) {
                        statement.execute("SET ROLE gridmind_workspace");
                    }
                }
                return connection;
            } catch (SQLException exception) {
                connection.close();
                throw exception;
            }
        }
    }
}

