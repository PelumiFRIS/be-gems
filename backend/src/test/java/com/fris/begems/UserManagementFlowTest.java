package com.fris.begems;

import static org.assertj.core.api.Assertions.assertThat;

import com.fris.begems.auth.dto.AuthResponse;
import com.fris.begems.auth.dto.LoginRequest;
import com.fris.begems.board.dto.BoardSummary;
import com.fris.begems.director.dto.DirectorSummary;
import com.fris.begems.support.IntegrationTestSupport;
import com.fris.begems.user.Role;
import com.fris.begems.user.UserStatus;
import com.fris.begems.user.dto.ChangePasswordRequest;
import com.fris.begems.user.dto.CreateUserRequest;
import com.fris.begems.user.dto.CreatedUserResponse;
import com.fris.begems.user.dto.TemporaryPasswordResponse;
import com.fris.begems.user.dto.UpdateUserRoleRequest;
import com.fris.begems.user.dto.UpdateUserStatusRequest;
import com.fris.begems.user.dto.UserSummary;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * An Org Admin creating staff accounts (with a one-time temporary password), changing
 * roles, disabling and re-enabling accounts, and resetting passwords; plus any user
 * changing their own password.
 */
class UserManagementFlowTest extends IntegrationTestSupport {

    @Test
    void orgAdminCanCreateAndManageStaffAccounts() {
        AuthResponse admin = signup(uniqueEmail(), "Users Org");
        String email = uniqueEmail();

        ResponseEntity<CreatedUserResponse> created = restTemplate.exchange("/api/users", HttpMethod.POST,
                authedRequest(admin.accessToken(), new CreateUserRequest("Deji", "Evaluator", email,
                        Role.EVALUATOR)),
                CreatedUserResponse.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.getBody().temporaryPassword()).hasSizeGreaterThan(12);
        UUID userId = created.getBody().user().id();
        AuthResponse evaluator = login(email, created.getBody().temporaryPassword());
        assertThat(evaluator.user().role()).isEqualTo(Role.EVALUATOR);

        // Directors are created through Board Setup, and SUPER_ADMIN can't be granted by an organisation.
        for (Role role : new Role[] {Role.DIRECTOR, Role.SUPER_ADMIN}) {
            ResponseEntity<String> rejected = restTemplate.exchange("/api/users", HttpMethod.POST,
                    authedRequest(admin.accessToken(), new CreateUserRequest("X", "Y", uniqueEmail(), role)),
                    String.class);
            assertThat(rejected.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        ResponseEntity<UserSummary> promoted = restTemplate.exchange("/api/users/" + userId + "/role",
                HttpMethod.PUT, authedRequest(admin.accessToken(), new UpdateUserRoleRequest(Role.COMPANY_SECRETARY)),
                UserSummary.class);
        assertThat(promoted.getBody().role()).isEqualTo(Role.COMPANY_SECRETARY);

        ResponseEntity<TemporaryPasswordResponse> reset = restTemplate.exchange(
                "/api/users/" + userId + "/reset-password", HttpMethod.POST, authedRequest(admin.accessToken()),
                TemporaryPasswordResponse.class);
        assertThat(reset.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(loginStatus(email, created.getBody().temporaryPassword())).isEqualTo(HttpStatus.UNAUTHORIZED);
        login(email, reset.getBody().temporaryPassword());

        ResponseEntity<UserSummary> disabled = restTemplate.exchange("/api/users/" + userId + "/status",
                HttpMethod.PUT, authedRequest(admin.accessToken(), new UpdateUserStatusRequest(UserStatus.DISABLED)),
                UserSummary.class);
        assertThat(disabled.getBody().status()).isEqualTo(UserStatus.DISABLED);
        assertThat(loginStatus(email, reset.getBody().temporaryPassword())).isEqualTo(HttpStatus.UNAUTHORIZED);
        // An existing session stops working as soon as the account is disabled.
        assertThat(restTemplate.exchange("/api/users/me", HttpMethod.GET, authedRequest(evaluator.accessToken()),
                String.class).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        restTemplate.exchange("/api/users/" + userId + "/status", HttpMethod.PUT,
                authedRequest(admin.accessToken(), new UpdateUserStatusRequest(UserStatus.ACTIVE)), UserSummary.class);
        login(email, reset.getBody().temporaryPassword());

        ResponseEntity<UserSummary[]> users = restTemplate.exchange("/api/users", HttpMethod.GET,
                authedRequest(admin.accessToken()), UserSummary[].class);
        assertThat(users.getBody()).extracting(UserSummary::email).contains(email, admin.user().email());
    }

    @Test
    void adminsCannotLockThemselvesOutOrReachOtherOrganisations() {
        AuthResponse admin = signup(uniqueEmail(), "Guardrails Org");
        UUID adminId = admin.user().id();

        assertThat(statusOf("/api/users/" + adminId + "/role", HttpMethod.PUT, admin.accessToken(),
                new UpdateUserRoleRequest(Role.EVALUATOR))).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(statusOf("/api/users/" + adminId + "/status", HttpMethod.PUT, admin.accessToken(),
                new UpdateUserStatusRequest(UserStatus.DISABLED))).isEqualTo(HttpStatus.BAD_REQUEST);

        AuthResponse otherAdmin = signup(uniqueEmail(), "Other Guardrails Org");
        assertThat(statusOf("/api/users/" + adminId + "/reset-password", HttpMethod.POST, otherAdmin.accessToken(),
                null)).isEqualTo(HttpStatus.NOT_FOUND);

        AuthResponse secretary = createCompanySecretaryAndLogin(admin.accessToken());
        assertThat(statusOf("/api/users", HttpMethod.GET, secretary.accessToken(), null))
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(statusOf("/api/users/" + adminId + "/reset-password", HttpMethod.POST, secretary.accessToken(),
                null)).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void directorAccountsCanBeResetButTheirRoleIsFixed() {
        AuthResponse admin = signup(uniqueEmail(), "Director Accounts Org");
        AuthResponse secretary = createCompanySecretaryAndLogin(admin.accessToken());
        BoardSummary board = createBoard(admin.accessToken(), "Board of Directors");
        DirectorSummary director = createDirector(admin.accessToken(), board.id(), "Kola Ade");
        AuthResponse directorLogin = inviteDirectorAndLogin(secretary.accessToken(), director);
        UUID directorUserId = directorLogin.user().id();

        assertThat(statusOf("/api/users/" + directorUserId + "/role", HttpMethod.PUT, admin.accessToken(),
                new UpdateUserRoleRequest(Role.ORG_ADMIN))).isEqualTo(HttpStatus.BAD_REQUEST);

        ResponseEntity<TemporaryPasswordResponse> reset = restTemplate.exchange(
                "/api/users/" + directorUserId + "/reset-password", HttpMethod.POST, authedRequest(admin.accessToken()),
                TemporaryPasswordResponse.class);
        assertThat(reset.getBody().email()).isEqualTo(director.email());
        login(director.email(), reset.getBody().temporaryPassword());
    }

    @Test
    void anyUserCanChangeTheirOwnPassword() {
        AuthResponse admin = signup(uniqueEmail(), "Password Org");
        String email = admin.user().email();

        assertThat(statusOf("/api/users/me/password", HttpMethod.PUT, admin.accessToken(),
                new ChangePasswordRequest("wrong-password", "new-password-1"))).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(statusOf("/api/users/me/password", HttpMethod.PUT, admin.accessToken(),
                new ChangePasswordRequest("password123", "short"))).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(statusOf("/api/users/me/password", HttpMethod.PUT, admin.accessToken(),
                new ChangePasswordRequest("password123", "password123"))).isEqualTo(HttpStatus.BAD_REQUEST);

        assertThat(statusOf("/api/users/me/password", HttpMethod.PUT, admin.accessToken(),
                new ChangePasswordRequest("password123", "new-password-1"))).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(loginStatus(email, "password123")).isEqualTo(HttpStatus.UNAUTHORIZED);
        login(email, "new-password-1");
    }

    private HttpStatus loginStatus(String email, String password) {
        return HttpStatus.valueOf(restTemplate.postForEntity("/api/auth/login", new LoginRequest(email, password),
                String.class).getStatusCode().value());
    }

    private HttpStatus statusOf(String url, HttpMethod method, String token, Object body) {
        return HttpStatus.valueOf(restTemplate.exchange(url, method, authedRequest(token, body), String.class)
                .getStatusCode().value());
    }
}
