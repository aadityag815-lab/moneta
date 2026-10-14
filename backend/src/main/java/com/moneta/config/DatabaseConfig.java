package com.moneta.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.net.URI;

@Configuration
public class DatabaseConfig {

    @Value("${DB_URL:#{null}}")
    private String dbUrl;

    @Value("${DATABASE_URL:#{null}}")
    private String databaseUrl;

    @Value("${spring.datasource.username:#{null}}")
    private String defaultUsername;

    @Value("${spring.datasource.password:#{null}}")
    private String defaultPassword;

    @Bean
    @Primary
    public DataSource dataSource() {
        HikariDataSource dataSource = new HikariDataSource();

        String raw = (dbUrl != null && !dbUrl.trim().isEmpty()) ? dbUrl : databaseUrl;

        if (raw == null || raw.trim().isEmpty()) {
            // Local fallback to persistent H2 in PostgreSQL mode
            dataSource.setDriverClassName("org.h2.Driver");
            dataSource.setJdbcUrl("jdbc:h2:file:./data/monetadb;DB_CLOSE_ON_EXIT=FALSE;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH");
            dataSource.setUsername(defaultUsername != null ? defaultUsername : "sa");
            dataSource.setPassword(defaultPassword != null ? defaultPassword : "");
            return dataSource;
        }

        String rawUrl = raw.trim();
        String jdbcUrl;
        String username = defaultUsername;
        String password = defaultPassword;

        // Render / Cloud PostgreSQL URL format: postgres://user:password@host:port/dbname
        // or postgresql://user:password@host:port/dbname
        if (rawUrl.startsWith("postgres://") || rawUrl.startsWith("postgresql://")) {
            try {
                // Normalize scheme for java.net.URI parser
                String uriString = rawUrl.startsWith("postgres://")
                        ? "http://" + rawUrl.substring("postgres://".length())
                        : "http://" + rawUrl.substring("postgresql://".length());

                URI uri = new URI(uriString);
                String userInfo = uri.getUserInfo();
                if (userInfo != null && userInfo.contains(":")) {
                    String[] credentials = userInfo.split(":", 2);
                    username = credentials[0];
                    password = credentials[1];
                }

                String host = uri.getHost();
                int port = uri.getPort() > 0 ? uri.getPort() : 5432;
                String path = uri.getPath(); // e.g. /dbname

                jdbcUrl = "jdbc:postgresql://" + host + ":" + port + path;
            } catch (Exception e) {
                // If URI parsing fails, simply prepend jdbc:
                jdbcUrl = rawUrl.startsWith("jdbc:") ? rawUrl : "jdbc:" + rawUrl;
            }
        } else if (!rawUrl.startsWith("jdbc:")) {
            jdbcUrl = "jdbc:" + rawUrl;
        } else {
            jdbcUrl = rawUrl;
        }

        dataSource.setDriverClassName("org.postgresql.Driver");
        dataSource.setJdbcUrl(jdbcUrl);
        if (username != null) {
            dataSource.setUsername(username);
        }
        if (password != null) {
            dataSource.setPassword(password);
        }

        return dataSource;
    }
}
