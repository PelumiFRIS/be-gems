package com.fris.begems.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.fris.begems.auth.dto.AuthResponse;
import com.fris.begems.organization.dto.OrganizationSignupRequest;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Runs against the Postgres started by the project's docker-compose.yml
 * (docker compose up -d postgres) rather than a Testcontainers-managed container —
 * same reasoning as board-portal's IntegrationTestSupport: Testcontainers' Docker
 * handshake is unreliable against some local Docker Desktop builds, while the
 * docker/docker compose CLI works fine against the same daemon.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
public abstract class IntegrationTestSupport {

    @Autowired
    protected TestRestTemplate restTemplate;

    protected AuthResponse signup(String email, String organizationName) {
        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                "/api/organizations/signup",
                new OrganizationSignupRequest(organizationName, "Ada", "Admin", email, "password123"),
                AuthResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody();
    }

    protected String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@example.com";
    }

    protected <T> HttpEntity<T> authedRequest(String token) {
        return authedRequest(token, null);
    }

    protected <T> HttpEntity<T> authedRequest(String token, T body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return new HttpEntity<>(body, headers);
    }
}
