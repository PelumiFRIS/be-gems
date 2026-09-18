package com.fris.begems.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.fris.begems.auth.dto.AuthResponse;
import com.fris.begems.auth.dto.LoginRequest;
import com.fris.begems.board.dto.BoardSummary;
import com.fris.begems.board.dto.CreateBoardRequest;
import com.fris.begems.director.DirectorClassification;
import com.fris.begems.director.dto.CreateDirectorRequest;
import com.fris.begems.director.dto.DirectorSummary;
import com.fris.begems.director.dto.InviteDirectorResponse;
import com.fris.begems.organization.dto.OrganizationSignupRequest;
import com.fris.begems.user.Role;
import com.fris.begems.user.dto.CreateUserRequest;
import com.fris.begems.user.dto.UserSummary;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
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

    /**
     * Director invitations and evaluation lifecycle management (create/launch/close, add
     * respondents, calculate scores) are Company Secretary/Evaluator-only — ORG_ADMIN
     * covers technical/account administration only. Use this wherever a test needs an
     * actor who can manage evaluations.
     */
    protected AuthResponse createCompanySecretaryAndLogin(String adminToken) {
        String email = uniqueEmail();
        ResponseEntity<UserSummary> response = restTemplate.exchange(
                "/api/users", HttpMethod.POST,
                authedRequest(adminToken, new CreateUserRequest("Board", "Secretary", email, "password123",
                        Role.COMPANY_SECRETARY)),
                UserSummary.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ResponseEntity<AuthResponse> loginResponse = restTemplate.postForEntity(
                "/api/auth/login", new LoginRequest(email, "password123"), AuthResponse.class);
        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        return loginResponse.getBody();
    }

    protected BoardSummary createBoard(String adminToken, String name) {
        ResponseEntity<BoardSummary> response = restTemplate.exchange(
                "/api/boards", HttpMethod.POST, authedRequest(adminToken, new CreateBoardRequest(name, null, null)),
                BoardSummary.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody();
    }

    protected DirectorSummary createDirector(String adminToken, UUID boardId, String name) {
        ResponseEntity<DirectorSummary> response = restTemplate.exchange(
                "/api/directors", HttpMethod.POST,
                authedRequest(adminToken, new CreateDirectorRequest(boardId, name, uniqueEmail(),
                        DirectorClassification.NON_EXECUTIVE_DIRECTOR, null, null)),
                DirectorSummary.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody();
    }

    protected AuthResponse inviteDirectorAndLogin(String adminToken, DirectorSummary director) {
        ResponseEntity<InviteDirectorResponse> inviteResponse = restTemplate.exchange(
                "/api/directors/" + director.id() + "/invite", HttpMethod.POST, authedRequest(adminToken),
                InviteDirectorResponse.class);
        assertThat(inviteResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<AuthResponse> loginResponse = restTemplate.postForEntity("/api/auth/login",
                new LoginRequest(director.email(), inviteResponse.getBody().temporaryPassword()), AuthResponse.class);
        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        return loginResponse.getBody();
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
