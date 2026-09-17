package com.fris.begems;

import static org.assertj.core.api.Assertions.assertThat;

import com.fris.begems.auth.dto.AuthResponse;
import com.fris.begems.board.dto.BoardSummary;
import com.fris.begems.board.dto.CreateBoardRequest;
import com.fris.begems.committee.CommitteeMemberRole;
import com.fris.begems.committee.dto.AddCommitteeMemberRequest;
import com.fris.begems.committee.dto.CommitteeSummary;
import com.fris.begems.committee.dto.CreateCommitteeRequest;
import com.fris.begems.director.DirectorClassification;
import com.fris.begems.director.dto.CreateDirectorRequest;
import com.fris.begems.director.dto.DirectorSummary;
import com.fris.begems.support.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class BoardDirectorCommitteeFlowTest extends IntegrationTestSupport {

    @Test
    void orgAdminCanStandUpABoardWithDirectorsAndACommittee() {
        AuthResponse admin = signup(uniqueEmail(), "Gateway Mortgage Bank");
        String token = admin.accessToken();

        ResponseEntity<BoardSummary> boardResponse = restTemplate.exchange(
                "/api/boards", HttpMethod.POST,
                authedRequest(token, new CreateBoardRequest("Board of Directors", null, null)), BoardSummary.class);
        assertThat(boardResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        var boardId = boardResponse.getBody().id();

        // A second board for the same org is out of MVP scope (one active board per org).
        ResponseEntity<String> secondBoard = restTemplate.exchange(
                "/api/boards", HttpMethod.POST,
                authedRequest(token, new CreateBoardRequest("Another Board", null, null)), String.class);
        assertThat(secondBoard.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        ResponseEntity<DirectorSummary> chairman = restTemplate.exchange(
                "/api/directors", HttpMethod.POST,
                authedRequest(token, new CreateDirectorRequest(boardId, "Ada Lovelace", "ada@example.com",
                        DirectorClassification.CHAIRMAN, null, null)),
                DirectorSummary.class);
        assertThat(chairman.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ResponseEntity<DirectorSummary> ined = restTemplate.exchange(
                "/api/directors", HttpMethod.POST,
                authedRequest(token, new CreateDirectorRequest(boardId, "Grace Hopper", "grace@example.com",
                        DirectorClassification.INDEPENDENT_NON_EXECUTIVE_DIRECTOR, null, null)),
                DirectorSummary.class);
        assertThat(ined.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ResponseEntity<DirectorSummary[]> directors = restTemplate.exchange(
                "/api/directors?boardId=" + boardId, HttpMethod.GET, authedRequest(token), DirectorSummary[].class);
        assertThat(directors.getBody()).hasSize(2);

        ResponseEntity<CommitteeSummary> committee = restTemplate.exchange(
                "/api/committees", HttpMethod.POST,
                authedRequest(token, new CreateCommitteeRequest(boardId, "Audit Committee", "Quarterly", "Oversees audit and controls")),
                CommitteeSummary.class);
        assertThat(committee.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(committee.getBody().members()).isEmpty();

        ResponseEntity<CommitteeSummary> withMember = restTemplate.exchange(
                "/api/committees/" + committee.getBody().id() + "/members", HttpMethod.POST,
                authedRequest(token, new AddCommitteeMemberRequest(ined.getBody().id(), CommitteeMemberRole.CHAIR)),
                CommitteeSummary.class);
        assertThat(withMember.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(withMember.getBody().members()).hasSize(1);
        assertThat(withMember.getBody().members().get(0).role()).isEqualTo(CommitteeMemberRole.CHAIR);
    }
}
