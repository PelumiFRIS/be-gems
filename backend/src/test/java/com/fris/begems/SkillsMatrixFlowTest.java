package com.fris.begems;

import static org.assertj.core.api.Assertions.assertThat;

import com.fris.begems.auth.dto.AuthResponse;
import com.fris.begems.board.dto.BoardSummary;
import com.fris.begems.director.dto.DirectorSummary;
import com.fris.begems.skill.RequiredLevel;
import com.fris.begems.skill.SkillCoverage;
import com.fris.begems.skill.dto.SkillRatingRequest;
import com.fris.begems.skill.dto.SkillRequest;
import com.fris.begems.skill.dto.SkillRow;
import com.fris.begems.skill.dto.SkillsMatrix;
import com.fris.begems.support.IntegrationTestSupport;
import com.fris.begems.user.Role;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class SkillsMatrixFlowTest extends IntegrationTestSupport {

    @Test
    void newBoardsStartWithTheMemoFrameworkAndRatingsDriveTheGapAnalysis() {
        AuthResponse admin = signup(uniqueEmail(), "Skills Org");
        BoardSummary board = createBoard(admin.accessToken(), "Board of Directors");
        DirectorSummary ada = createDirector(admin.accessToken(), board.id(), "Ada Lovelace");
        DirectorSummary grace = createDirector(admin.accessToken(), board.id(), "Grace Hopper");

        SkillsMatrix fresh = matrix(admin.accessToken(), board.id());
        assertThat(fresh.skills()).hasSize(18);
        assertThat(fresh.skills().get(0).name()).isEqualTo("Strategy");
        assertThat(fresh.skills()).allSatisfy(s -> assertThat(s.coverage()).isEqualTo(SkillCoverage.NOT_ASSESSED));
        assertThat(fresh.directors()).extracting(d -> d.name()).containsExactly("Ada Lovelace", "Grace Hopper");

        String token = admin.accessToken();
        rate(token, skill(fresh, "Strategy"), ada.id(), 5);
        rate(token, skill(fresh, "Strategy"), grace.id(), 4);
        rate(token, skill(fresh, "Finance"), ada.id(), 4);
        rate(token, skill(fresh, "Finance"), grace.id(), 2);
        rate(token, skill(fresh, "Audit"), ada.id(), 3);
        rate(token, skill(fresh, "Audit"), grace.id(), 3);
        rate(token, skill(fresh, "Banking"), ada.id(), 3);
        rate(token, skill(fresh, "Marketing"), ada.id(), 3);
        SkillsMatrix rated = rate(token, skill(fresh, "Human resources"), ada.id(), 2);

        assertThat(row(rated, "Strategy").coverage()).isEqualTo(SkillCoverage.COVERED);
        assertThat(row(rated, "Strategy").average()).isEqualTo(4.5);
        assertThat(row(rated, "Strategy").ratings()).containsEntry(ada.id(), 5).containsEntry(grace.id(), 4);
        assertThat(row(rated, "Finance").coverage()).isEqualTo(SkillCoverage.SINGLE_PERSON_DEPENDENCY);
        assertThat(row(rated, "Finance").proficientDirectors()).containsExactly("Ada Lovelace");
        assertThat(row(rated, "Audit").coverage()).isEqualTo(SkillCoverage.CRITICAL_GAP);
        assertThat(row(rated, "Banking").coverage()).isEqualTo(SkillCoverage.UNDERREPRESENTED);
        assertThat(row(rated, "Marketing").coverage()).isEqualTo(SkillCoverage.COVERED);
        assertThat(row(rated, "Human resources").coverage()).isEqualTo(SkillCoverage.UNDERREPRESENTED);
        assertThat(rated.coverageCounts()).containsEntry(SkillCoverage.CRITICAL_GAP, 1L)
                .containsEntry(SkillCoverage.SINGLE_PERSON_DEPENDENCY, 1L)
                .containsEntry(SkillCoverage.UNDERREPRESENTED, 2L)
                .containsEntry(SkillCoverage.COVERED, 2L)
                .containsEntry(SkillCoverage.NOT_ASSESSED, 12L);

        SkillsMatrix cleared = rate(token, skill(fresh, "Banking"), ada.id(), null);
        assertThat(row(cleared, "Banking").coverage()).isEqualTo(SkillCoverage.NOT_ASSESSED);
        assertThat(row(cleared, "Banking").ratings()).isEmpty();

        ResponseEntity<String> outOfRange = restTemplate.exchange(
                "/api/skills/" + skill(fresh, "Audit") + "/ratings/" + ada.id(), HttpMethod.PUT,
                authedRequest(token, new SkillRatingRequest(6)), String.class);
        assertThat(outOfRange.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        // Removing a director takes their ratings with them.
        ResponseEntity<Void> removed = restTemplate.exchange("/api/directors/" + grace.id(), HttpMethod.DELETE,
                authedRequest(token), Void.class);
        assertThat(removed.getStatusCode().is2xxSuccessful()).isTrue();
        SkillsMatrix afterRemoval = matrix(token, board.id());
        assertThat(row(afterRemoval, "Strategy").ratings()).containsOnlyKeys(ada.id());
        assertThat(row(afterRemoval, "Strategy").coverage()).isEqualTo(SkillCoverage.SINGLE_PERSON_DEPENDENCY);
    }

    @Test
    void theFrameworkIsConfigurable() {
        AuthResponse admin = signup(uniqueEmail(), "Skills Config Org");
        BoardSummary board = createBoard(admin.accessToken(), "Board of Directors");
        String token = admin.accessToken();

        ResponseEntity<SkillsMatrix> added = restTemplate.exchange("/api/boards/" + board.id() + "/skills",
                HttpMethod.POST, authedRequest(token, new SkillRequest("  Insurance ", RequiredLevel.HIGH, true)),
                SkillsMatrix.class);
        assertThat(added.getStatusCode()).isEqualTo(HttpStatus.OK);
        SkillRow insurance = row(added.getBody(), "Insurance");
        assertThat(insurance.displayOrder()).isEqualTo(19);
        assertThat(insurance.futureFocus()).isTrue();

        ResponseEntity<String> duplicate = restTemplate.exchange("/api/boards/" + board.id() + "/skills",
                HttpMethod.POST, authedRequest(token, new SkillRequest("strategy", RequiredLevel.LOW, false)),
                String.class);
        assertThat(duplicate.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        ResponseEntity<SkillsMatrix> updated = restTemplate.exchange("/api/skills/" + insurance.id(),
                HttpMethod.PUT, authedRequest(token, new SkillRequest("Insurance", RequiredLevel.MEDIUM, false)),
                SkillsMatrix.class);
        assertThat(row(updated.getBody(), "Insurance").requiredLevel()).isEqualTo(RequiredLevel.MEDIUM);

        ResponseEntity<SkillsMatrix> deleted = restTemplate.exchange("/api/skills/" + insurance.id(),
                HttpMethod.DELETE, authedRequest(token), SkillsMatrix.class);
        assertThat(deleted.getBody().skills()).extracting(SkillRow::name).doesNotContain("Insurance").hasSize(18);
    }

    @Test
    void theMatrixIsStaffOnlyAndOrganisationScoped() {
        AuthResponse admin = signup(uniqueEmail(), "Skills Access Org");
        AuthResponse cs = createCompanySecretaryAndLogin(admin.accessToken());
        AuthResponse evaluator = createUserAndLogin(admin.accessToken(), "Eve", "Evaluator", Role.EVALUATOR);
        BoardSummary board = createBoard(admin.accessToken(), "Board of Directors");
        DirectorSummary director = createDirector(admin.accessToken(), board.id(), "Ada Lovelace");
        AuthResponse directorLogin = inviteDirectorAndLogin(cs.accessToken(), director);
        UUID strategy = skill(matrix(admin.accessToken(), board.id()), "Strategy");

        assertThat(rate(cs.accessToken(), strategy, director.id(), 4).skills()).isNotEmpty();
        assertThat(matrix(evaluator.accessToken(), board.id()).skills()).isNotEmpty();

        ResponseEntity<String> evaluatorRates = restTemplate.exchange(
                "/api/skills/" + strategy + "/ratings/" + director.id(), HttpMethod.PUT,
                authedRequest(evaluator.accessToken(), new SkillRatingRequest(5)), String.class);
        assertThat(evaluatorRates.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        ResponseEntity<String> directorViews = restTemplate.exchange(
                "/api/boards/" + board.id() + "/skills-matrix", HttpMethod.GET,
                authedRequest(directorLogin.accessToken()), String.class);
        assertThat(directorViews.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        AuthResponse otherOrg = signup(uniqueEmail(), "Skills Other Org");
        ResponseEntity<String> crossOrgView = restTemplate.exchange(
                "/api/boards/" + board.id() + "/skills-matrix", HttpMethod.GET,
                authedRequest(otherOrg.accessToken()), String.class);
        assertThat(crossOrgView.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        ResponseEntity<String> crossOrgRate = restTemplate.exchange(
                "/api/skills/" + strategy + "/ratings/" + director.id(), HttpMethod.PUT,
                authedRequest(otherOrg.accessToken(), new SkillRatingRequest(1)), String.class);
        assertThat(crossOrgRate.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private SkillsMatrix matrix(String token, UUID boardId) {
        ResponseEntity<SkillsMatrix> response = restTemplate.exchange(
                "/api/boards/" + boardId + "/skills-matrix", HttpMethod.GET, authedRequest(token),
                SkillsMatrix.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody();
    }

    private SkillsMatrix rate(String token, UUID skillId, UUID directorId, Integer rating) {
        ResponseEntity<SkillsMatrix> response = restTemplate.exchange(
                "/api/skills/" + skillId + "/ratings/" + directorId, HttpMethod.PUT,
                authedRequest(token, new SkillRatingRequest(rating)), SkillsMatrix.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody();
    }

    private static UUID skill(SkillsMatrix matrix, String name) {
        return row(matrix, name).id();
    }

    private static SkillRow row(SkillsMatrix matrix, String name) {
        return matrix.skills().stream().filter(s -> s.name().equals(name)).findFirst().orElseThrow();
    }
}
