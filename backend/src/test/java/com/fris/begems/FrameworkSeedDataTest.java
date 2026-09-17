package com.fris.begems;

import static org.assertj.core.api.Assertions.assertThat;

import com.fris.begems.auth.dto.AuthResponse;
import com.fris.begems.framework.dto.FrameworkDetail;
import com.fris.begems.framework.dto.QuestionSummary;
import com.fris.begems.support.IntegrationTestSupport;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Guards the V2 migration's seed data: 18 dimensions whose weights sum to exactly
 * 100% (memo section 17's validation requirement), and both example questionnaires
 * from the memo present with their full question counts.
 */
class FrameworkSeedDataTest extends IntegrationTestSupport {

    @Test
    void nccg2018IsSeededWithEighteenDimensionsWeightedToOneHundredPercent() {
        AuthResponse user = signup(uniqueEmail(), "Any Org");

        ResponseEntity<FrameworkDetail> response = restTemplate.exchange(
                "/api/frameworks/active", HttpMethod.GET, authedRequest(user.accessToken()), FrameworkDetail.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        FrameworkDetail framework = response.getBody();
        assertThat(framework.code()).isEqualTo("NCCG_2018");
        assertThat(framework.dimensions()).hasSize(18);

        BigDecimal totalWeight = framework.dimensions().stream()
                .map(d -> d.defaultWeightPct())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(totalWeight).isEqualByComparingTo("100.00");
    }

    @Test
    void boardAndDirectorPeerQuestionnairesAreBothSeeded() {
        AuthResponse user = signup(uniqueEmail(), "Any Org");

        ResponseEntity<QuestionSummary[]> boardQuestions = restTemplate.exchange(
                "/api/frameworks/active/questions?evaluationType=BOARD", HttpMethod.GET,
                authedRequest(user.accessToken()), QuestionSummary[].class);
        assertThat(boardQuestions.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(boardQuestions.getBody()).hasSize(58);

        ResponseEntity<QuestionSummary[]> peerQuestions = restTemplate.exchange(
                "/api/frameworks/active/questions?evaluationType=DIRECTOR_PEER", HttpMethod.GET,
                authedRequest(user.accessToken()), QuestionSummary[].class);
        assertThat(peerQuestions.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(peerQuestions.getBody()).hasSize(28);
    }
}
