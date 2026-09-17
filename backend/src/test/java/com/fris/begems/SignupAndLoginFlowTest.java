package com.fris.begems;

import static org.assertj.core.api.Assertions.assertThat;

import com.fris.begems.auth.dto.AuthResponse;
import com.fris.begems.auth.dto.LoginRequest;
import com.fris.begems.support.IntegrationTestSupport;
import com.fris.begems.user.dto.UserSummary;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class SignupAndLoginFlowTest extends IntegrationTestSupport {

    @Test
    void signupCreatesAnOrgAdminWhoCanThenLoginAndFetchTheirOwnProfile() {
        String email = uniqueEmail();
        AuthResponse signup = signup(email, "Gateway Mortgage Bank");
        assertThat(signup.accessToken()).isNotBlank();
        assertThat(signup.user().role().toString()).isEqualTo("ORG_ADMIN");
        assertThat(signup.user().organizationName()).isEqualTo("Gateway Mortgage Bank");

        ResponseEntity<AuthResponse> login = restTemplate.postForEntity(
                "/api/auth/login", new LoginRequest(email, "password123"), AuthResponse.class);
        assertThat(login.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(login.getBody().accessToken()).isNotBlank();

        ResponseEntity<UserSummary> me = restTemplate.exchange(
                "/api/users/me", HttpMethod.GET, authedRequest(login.getBody().accessToken()), UserSummary.class);
        assertThat(me.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(me.getBody().email()).isEqualTo(email);
    }

    @Test
    void signupRejectsADuplicateEmail() {
        String email = uniqueEmail();
        signup(email, "First Org");

        ResponseEntity<String> secondSignup = restTemplate.postForEntity(
                "/api/organizations/signup",
                new com.fris.begems.organization.dto.OrganizationSignupRequest(
                        "Second Org", "Bola", "Admin", email, "password123"),
                String.class);
        assertThat(secondSignup.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void wrongPasswordIsRejected() {
        String email = uniqueEmail();
        signup(email, "An Org");

        ResponseEntity<String> login = restTemplate.postForEntity(
                "/api/auth/login", new LoginRequest(email, "wrong-password"), String.class);
        assertThat(login.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
