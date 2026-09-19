package com.fris.begems.config;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Hosting providers such as Neon hand out a single "postgresql://user:pass@host/db?..."
 * connection string, but pgjdbc has no support for credentials embedded in the URI
 * authority - it needs spring.datasource.username/password set separately. Rather than
 * ask whoever configures the deployment to split that string into pieces by hand, this
 * parses DATABASE_URL (when present) into spring.datasource.url/username/password at
 * boot, so hosting only ever needs to hold the one connection string Neon/Render/etc.
 * already give it. Local/docker-compose runs never set DATABASE_URL, so
 * application.yml's discrete DB_HOST/DB_PORT/DB_NAME/DB_USER/DB_PASSWORD keep working
 * unchanged.
 */
public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String databaseUrl = environment.getProperty("DATABASE_URL");
        if (databaseUrl == null || databaseUrl.isBlank()) {
            return;
        }
        environment.getPropertySources().addFirst(new MapPropertySource("databaseUrl", toDatasourceProperties(databaseUrl)));
    }

    static Map<String, Object> toDatasourceProperties(String databaseUrl) {
        URI uri = URI.create(databaseUrl);
        String userInfo = uri.getUserInfo();
        if (userInfo == null || !userInfo.contains(":")) {
            throw new IllegalArgumentException("DATABASE_URL must include user:password credentials");
        }
        int separator = userInfo.indexOf(':');
        String username = userInfo.substring(0, separator);
        String password = userInfo.substring(separator + 1);
        int port = uri.getPort() == -1 ? 5432 : uri.getPort();

        StringBuilder jdbcUrl = new StringBuilder("jdbc:postgresql://")
                .append(uri.getHost()).append(':').append(port).append(uri.getPath());
        String query = uri.getQuery();
        // Only sslmode is meaningful to pgjdbc; libpq-only params like channel_binding
        // would otherwise make it reject the connection with an unknown-parameter error.
        if (query != null && query.contains("sslmode=require")) {
            jdbcUrl.append("?sslmode=require");
        }

        Map<String, Object> props = new LinkedHashMap<>();
        props.put("spring.datasource.url", jdbcUrl.toString());
        props.put("spring.datasource.username", username);
        props.put("spring.datasource.password", password);
        return props;
    }
}
