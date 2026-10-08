package com.fris.begems;

import static org.assertj.core.api.Assertions.assertThat;

import com.fris.begems.auth.dto.AuthResponse;
import com.fris.begems.board.dto.BoardSummary;
import com.fris.begems.evaluation.dto.CreateEvaluationRequest;
import com.fris.begems.framework.EvaluationType;
import com.fris.begems.support.IntegrationTestSupport;
import com.fris.begems.user.Role;
import com.fris.begems.user.dto.UpdateCompanySecretaryAccessRequest;
import com.fris.begems.user.dto.UpdateUserRoleRequest;
import com.fris.begems.user.dto.UserSummary;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * An Organisation Administrator with Company Secretary access holds both roles on
 * one login. The founding admin gets it at signup, since they are usually the
 * organisation's Company Secretary.
 */
class CompanySecretaryAccessFlowTest extends IntegrationTestSupport {

    @Test
    void foundingAdminCanRunEvaluationsAndOtherAdminsNeedAccessGranted() {
        AuthResponse founder = signup(uniqueEmail(), "Dual Role Org");
        assertThat(founder.user().companySecretaryAccess()).isTrue();
        BoardSummary board = createBoard(founder.accessToken(), "Board of Directors");

        assertThat(createEvaluation(founder.accessToken(), board).getStatusCode()).isEqualTo(HttpStatus.CREATED);

        AuthResponse secondAdmin = createUserAndLogin(founder.accessToken(), "Second", "Admin", Role.ORG_ADMIN);
        assertThat(secondAdmin.user().companySecretaryAccess()).isFalse();
        assertThat(createEvaluation(secondAdmin.accessToken(), board).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);

        UserSummary granted = setCompanySecretaryAccess(founder.accessToken(), secondAdmin.user().id(), true);
        assertThat(granted.companySecretaryAccess()).isTrue();
        assertThat(createEvaluation(secondAdmin.accessToken(), board).getStatusCode())
                .isEqualTo(HttpStatus.CREATED);

        // An admin can switch their own access off (and back on) without another admin.
        setCompanySecretaryAccess(secondAdmin.accessToken(), secondAdmin.user().id(), false);
        assertThat(createEvaluation(secondAdmin.accessToken(), board).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        setCompanySecretaryAccess(secondAdmin.accessToken(), secondAdmin.user().id(), true);
        assertThat(createEvaluation(secondAdmin.accessToken(), board).getStatusCode())
                .isEqualTo(HttpStatus.CREATED);

        // Moving off ORG_ADMIN drops the extra access rather than leaving it dormant.
        ResponseEntity<UserSummary> demoted = restTemplate.exchange(
                "/api/users/" + secondAdmin.user().id() + "/role", HttpMethod.PUT,
                authedRequest(founder.accessToken(), new UpdateUserRoleRequest(Role.EVALUATOR)), UserSummary.class);
        assertThat(demoted.getBody().companySecretaryAccess()).isFalse();
        ResponseEntity<UserSummary> promotedBack = restTemplate.exchange(
                "/api/users/" + secondAdmin.user().id() + "/role", HttpMethod.PUT,
                authedRequest(founder.accessToken(), new UpdateUserRoleRequest(Role.ORG_ADMIN)), UserSummary.class);
        assertThat(promotedBack.getBody().companySecretaryAccess()).isFalse();
    }

    @Test
    void accessOnlyAppliesToAdminsAndOnlyAdminsCanGrantIt() {
        AuthResponse founder = signup(uniqueEmail(), "Dual Role Org 2");
        AuthResponse cs = createCompanySecretaryAndLogin(founder.accessToken());
        AuthResponse evaluator = createUserAndLogin(founder.accessToken(), "Eve", "Evaluator", Role.EVALUATOR);

        ResponseEntity<String> onEvaluator = restTemplate.exchange(
                "/api/users/" + evaluator.user().id() + "/company-secretary-access", HttpMethod.PUT,
                authedRequest(founder.accessToken(), new UpdateCompanySecretaryAccessRequest(true)), String.class);
        assertThat(onEvaluator.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        ResponseEntity<String> byCompanySecretary = restTemplate.exchange(
                "/api/users/" + founder.user().id() + "/company-secretary-access", HttpMethod.PUT,
                authedRequest(cs.accessToken(), new UpdateCompanySecretaryAccessRequest(false)), String.class);
        assertThat(byCompanySecretary.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        AuthResponse otherOrg = signup(uniqueEmail(), "Dual Role Other Org");
        ResponseEntity<String> crossOrg = restTemplate.exchange(
                "/api/users/" + founder.user().id() + "/company-secretary-access", HttpMethod.PUT,
                authedRequest(otherOrg.accessToken(), new UpdateCompanySecretaryAccessRequest(false)), String.class);
        assertThat(crossOrg.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private ResponseEntity<String> createEvaluation(String token, BoardSummary board) {
        return restTemplate.exchange("/api/evaluations", HttpMethod.POST,
                authedRequest(token, new CreateEvaluationRequest(board.id(), EvaluationType.BOARD, null, 2026)),
                String.class);
    }
}
