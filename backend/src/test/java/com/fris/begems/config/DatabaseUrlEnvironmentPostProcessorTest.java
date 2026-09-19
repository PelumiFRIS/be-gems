package com.fris.begems.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import org.junit.jupiter.api.Test;

class DatabaseUrlEnvironmentPostProcessorTest {

    @Test
    void parsesNeonStyleUrlWithSslmodeAndDropsUnsupportedParams() {
        Map<String, Object> props = DatabaseUrlEnvironmentPostProcessor.toDatasourceProperties(
                "postgresql://neondb_owner:s3cr3t-pass@ep-late-violet-b2r4k59u-pooler.c-6.eu-central-1.aws.neon.tech/neondb?sslmode=require&channel_binding=require");

        assertThat(props.get("spring.datasource.url")).isEqualTo(
                "jdbc:postgresql://ep-late-violet-b2r4k59u-pooler.c-6.eu-central-1.aws.neon.tech:5432/neondb?sslmode=require");
        assertThat(props.get("spring.datasource.username")).isEqualTo("neondb_owner");
        assertThat(props.get("spring.datasource.password")).isEqualTo("s3cr3t-pass");
    }

    @Test
    void defaultsToPort5432WhenNoneSpecified() {
        Map<String, Object> props = DatabaseUrlEnvironmentPostProcessor.toDatasourceProperties(
                "postgresql://user:pass@db.example.com/mydb");

        assertThat(props.get("spring.datasource.url")).isEqualTo("jdbc:postgresql://db.example.com:5432/mydb");
    }

    @Test
    void respectsExplicitPort() {
        Map<String, Object> props = DatabaseUrlEnvironmentPostProcessor.toDatasourceProperties(
                "postgresql://user:pass@db.example.com:6543/mydb");

        assertThat(props.get("spring.datasource.url")).isEqualTo("jdbc:postgresql://db.example.com:6543/mydb");
    }

    @Test
    void rejectsUrlWithoutCredentials() {
        assertThatThrownBy(() -> DatabaseUrlEnvironmentPostProcessor.toDatasourceProperties(
                "postgresql://db.example.com/mydb"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
