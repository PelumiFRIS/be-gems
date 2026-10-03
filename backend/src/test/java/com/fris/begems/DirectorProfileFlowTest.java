package com.fris.begems;

import static org.assertj.core.api.Assertions.assertThat;

import com.fris.begems.auth.dto.AuthResponse;
import com.fris.begems.board.dto.BoardSummary;
import com.fris.begems.committee.CommitteeMemberRole;
import com.fris.begems.committee.dto.AddCommitteeMemberRequest;
import com.fris.begems.committee.dto.CommitteeMemberSummary;
import com.fris.begems.committee.dto.CommitteeSummary;
import com.fris.begems.committee.dto.CreateCommitteeRequest;
import com.fris.begems.director.DirectorClassification;
import com.fris.begems.director.dto.CreateDirectorRequest;
import com.fris.begems.director.dto.DirectorCvSummary;
import com.fris.begems.director.dto.DirectorDetail;
import com.fris.begems.director.dto.DirectorSummary;
import com.fris.begems.director.dto.UpdateDirectorRequest;
import com.fris.begems.evaluation.ConfidentialityMode;
import com.fris.begems.evaluation.dto.AddRespondentRequest;
import com.fris.begems.evaluation.dto.CreateEvaluationRequest;
import com.fris.begems.evaluation.dto.EvaluationDetail;
import com.fris.begems.evaluation.dto.EvaluationSummary;
import com.fris.begems.framework.EvaluationType;
import com.fris.begems.support.IntegrationTestSupport;
import com.fris.begems.user.dto.UserSummary;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * Covers the first round of Board Setup review feedback: director biodata with a
 * required email, duplicate-submission protection, editing and removing directors,
 * CV upload, and per-director committee membership with one chair per committee.
 */
class DirectorProfileFlowTest extends IntegrationTestSupport {

    @Test
    void directorBiodataCanBeCapturedViewedAndEdited() {
        AuthResponse admin = signup(uniqueEmail(), "Biodata Org");
        BoardSummary board = createBoard(admin.accessToken(), "Board of Directors");

        CreateDirectorRequest request = new CreateDirectorRequest(board.id(), "Adaeze Okafor", "adaeze@example.com",
                DirectorClassification.MD_CEO, LocalDate.of(2019, 4, 1), LocalDate.of(2027, 3, 31),
                LocalDate.of(2023, 5, 10), LocalDate.of(1972, 8, 15), "+234 803 000 0000", "12 Marina, Lagos",
                "Chartered Accountant", "B.Sc Accounting, University of Lagos; FCA",
                "25 years in banking and capital markets");
        ResponseEntity<DirectorSummary> created = restTemplate.exchange("/api/directors", HttpMethod.POST,
                authedRequest(admin.accessToken(), request), DirectorSummary.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.getBody().classification()).isEqualTo(DirectorClassification.MD_CEO);
        UUID directorId = created.getBody().id();

        DirectorDetail detail = getDetail(admin.accessToken(), directorId).getBody();
        assertThat(detail.dateOfBirth()).isEqualTo(LocalDate.of(1972, 8, 15));
        assertThat(detail.reElectionDate()).isEqualTo(LocalDate.of(2023, 5, 10));
        assertThat(detail.phone()).isEqualTo("+234 803 000 0000");
        assertThat(detail.profession()).isEqualTo("Chartered Accountant");
        assertThat(detail.qualification()).contains("FCA");
        assertThat(detail.experience()).contains("capital markets");
        assertThat(detail.cv()).isNull();
        assertThat(detail.committees()).isEmpty();

        UpdateDirectorRequest update = new UpdateDirectorRequest("Adaeze N. Okafor", "adaeze.okafor@example.com",
                DirectorClassification.CHAIRMAN, LocalDate.of(2019, 4, 1), null, LocalDate.of(2025, 5, 12), null,
                "", "14 Marina, Lagos", "Chartered Accountant", null, null);
        ResponseEntity<DirectorDetail> updated = restTemplate.exchange("/api/directors/" + directorId,
                HttpMethod.PUT, authedRequest(admin.accessToken(), update), DirectorDetail.class);
        assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updated.getBody().name()).isEqualTo("Adaeze N. Okafor");
        assertThat(updated.getBody().email()).isEqualTo("adaeze.okafor@example.com");
        assertThat(updated.getBody().classification()).isEqualTo(DirectorClassification.CHAIRMAN);
        assertThat(updated.getBody().reElectionDate()).isEqualTo(LocalDate.of(2025, 5, 12));
        assertThat(updated.getBody().phone()).isNull();
        assertThat(updated.getBody().address()).isEqualTo("14 Marina, Lagos");

        // Email is required on both create and edit.
        ResponseEntity<String> noEmail = restTemplate.exchange("/api/directors", HttpMethod.POST,
                authedRequest(admin.accessToken(), directorRequest(board.id(), "No Email", null,
                        DirectorClassification.NON_EXECUTIVE_DIRECTOR)),
                String.class);
        assertThat(noEmail.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ResponseEntity<String> blankEmailEdit = restTemplate.exchange("/api/directors/" + directorId,
                HttpMethod.PUT, authedRequest(admin.accessToken(), new UpdateDirectorRequest("Adaeze", " ",
                        DirectorClassification.CHAIRMAN, null, null, null, null, null, null, null, null, null)),
                String.class);
        assertThat(blankEmailEdit.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void duplicateSubmissionsDoNotCreateDuplicateDirectors() throws Exception {
        AuthResponse admin = signup(uniqueEmail(), "Duplicate Org");
        BoardSummary board = createBoard(admin.accessToken(), "Board of Directors");
        CreateDirectorRequest request = directorRequest(board.id(), "Tunde Bakare", "tunde@example.com",
                DirectorClassification.NON_EXECUTIVE_DIRECTOR);

        // Simulates the same form being submitted repeatedly while the server is slow to respond.
        ExecutorService pool = Executors.newFixedThreadPool(5);
        try {
            List<CompletableFuture<HttpStatus>> attempts = IntStream.range(0, 5)
                    .mapToObj(i -> CompletableFuture.supplyAsync(() -> HttpStatus.valueOf(restTemplate
                            .exchange("/api/directors", HttpMethod.POST, authedRequest(admin.accessToken(), request),
                                    String.class)
                            .getStatusCode().value()), pool))
                    .toList();
            List<HttpStatus> statuses = attempts.stream().map(CompletableFuture::join).toList();
            assertThat(statuses).containsOnlyOnce(HttpStatus.CREATED);
            assertThat(statuses).filteredOn(s -> s != HttpStatus.CREATED).containsOnly(HttpStatus.CONFLICT);
        } finally {
            pool.shutdown();
        }

        // Email matching is case-insensitive.
        ResponseEntity<String> differentCase = restTemplate.exchange("/api/directors", HttpMethod.POST,
                authedRequest(admin.accessToken(), directorRequest(board.id(), "Tunde Bakare", "TUNDE@example.com",
                        DirectorClassification.NON_EXECUTIVE_DIRECTOR)),
                String.class);
        assertThat(differentCase.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        ResponseEntity<DirectorSummary[]> directors = restTemplate.exchange("/api/directors?boardId=" + board.id(),
                HttpMethod.GET, authedRequest(admin.accessToken()), DirectorSummary[].class);
        assertThat(directors.getBody()).hasSize(1);

        // Editing another director onto an email already on the board is also rejected.
        DirectorSummary other = createDirector(admin.accessToken(), board.id(), "Ngozi Eze");
        ResponseEntity<String> clash = restTemplate.exchange("/api/directors/" + other.id(), HttpMethod.PUT,
                authedRequest(admin.accessToken(), new UpdateDirectorRequest("Ngozi Eze", "tunde@example.com",
                        DirectorClassification.NON_EXECUTIVE_DIRECTOR, null, null, null, null, null, null, null,
                        null, null)),
                String.class);
        assertThat(clash.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void directorsCanBeRemovedUnlessTheyArePartOfAnEvaluation() {
        AuthResponse admin = signup(uniqueEmail(), "Removal Org");
        AuthResponse cs = createCompanySecretaryAndLogin(admin.accessToken());
        BoardSummary board = createBoard(admin.accessToken(), "Board of Directors");
        DirectorSummary duplicate = createDirector(admin.accessToken(), board.id(), "Duplicate Entry");
        DirectorSummary respondent = createDirector(admin.accessToken(), board.id(), "Evaluated Director");

        CommitteeSummary committee = createCommittee(admin.accessToken(), board.id(), "Audit Committee");
        assignMember(admin.accessToken(), committee.id(), duplicate.id(), CommitteeMemberRole.MEMBER);
        uploadCv(admin.accessToken(), duplicate.id(), "cv.pdf", "application/pdf", "cv".getBytes(StandardCharsets.UTF_8));
        inviteDirectorAndLogin(cs.accessToken(), duplicate);

        ResponseEntity<Void> removed = restTemplate.exchange("/api/directors/" + duplicate.id(), HttpMethod.DELETE,
                authedRequest(cs.accessToken()), Void.class);
        assertThat(removed.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(detailStatus(admin.accessToken(), duplicate.id())).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(listCommittees(admin.accessToken(), board.id()).get(0).members()).isEmpty();

        // Their portal login is removed too, so the same person can be added and invited again.
        DirectorSummary readded = createDirectorWithEmail(admin.accessToken(), board.id(), "Duplicate Entry",
                duplicate.email());
        inviteDirectorAndLogin(cs.accessToken(), readded);

        ResponseEntity<EvaluationSummary> evaluation = restTemplate.exchange("/api/evaluations", HttpMethod.POST,
                authedRequest(cs.accessToken(), new CreateEvaluationRequest(board.id(), EvaluationType.BOARD, null,
                        2026)),
                EvaluationSummary.class);
        restTemplate.exchange("/api/evaluations/" + evaluation.getBody().id() + "/respondents", HttpMethod.POST,
                authedRequest(cs.accessToken(), new AddRespondentRequest(respondent.id(),
                        ConfidentialityMode.CONFIDENTIAL)),
                EvaluationDetail.class);

        ResponseEntity<String> blocked = restTemplate.exchange("/api/directors/" + respondent.id(),
                HttpMethod.DELETE, authedRequest(cs.accessToken()), String.class);
        assertThat(blocked.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void editingAnInvitedDirectorsEmailMovesTheirPortalLogin() {
        AuthResponse admin = signup(uniqueEmail(), "Login Sync Org");
        AuthResponse cs = createCompanySecretaryAndLogin(admin.accessToken());
        BoardSummary board = createBoard(admin.accessToken(), "Board of Directors");
        DirectorSummary director = createDirector(admin.accessToken(), board.id(), "Kemi Adeyemi");
        AuthResponse directorLogin = inviteDirectorAndLogin(cs.accessToken(), director);

        // A director can read their own biodata but not a colleague's.
        DirectorSummary colleague = createDirector(admin.accessToken(), board.id(), "Colleague Director");
        assertThat(detailStatus(directorLogin.accessToken(), director.id())).isEqualTo(HttpStatus.OK);
        assertThat(detailStatus(directorLogin.accessToken(), colleague.id())).isEqualTo(HttpStatus.FORBIDDEN);
        ResponseEntity<String> directorEdit = restTemplate.exchange("/api/directors/" + director.id(), HttpMethod.PUT,
                authedRequest(directorLogin.accessToken(), new UpdateDirectorRequest("Kemi Adeyemi", director.email(),
                        DirectorClassification.CHAIRMAN, null, null, null, null, null, null, null, null, null)),
                String.class);
        assertThat(directorEdit.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        String newEmail = uniqueEmail();
        ResponseEntity<DirectorDetail> updated = restTemplate.exchange("/api/directors/" + director.id(),
                HttpMethod.PUT, authedRequest(cs.accessToken(), new UpdateDirectorRequest("Kemi Adeyemi", newEmail,
                        DirectorClassification.NON_EXECUTIVE_DIRECTOR, null, null, null, null, null, null, null, null,
                        null)),
                DirectorDetail.class);
        assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updated.getBody().hasPortalAccess()).isTrue();

        // Another account's email can't be taken over through a director edit.
        ResponseEntity<String> takeover = restTemplate.exchange("/api/directors/" + director.id(), HttpMethod.PUT,
                authedRequest(cs.accessToken(), new UpdateDirectorRequest("Kemi Adeyemi", cs.user().email(),
                        DirectorClassification.NON_EXECUTIVE_DIRECTOR, null, null, null, null, null, null, null, null,
                        null)),
                String.class);
        assertThat(takeover.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        ResponseEntity<UserSummary> me = restTemplate.exchange("/api/users/me", HttpMethod.GET,
                authedRequest(directorLogin.accessToken()), UserSummary.class);
        assertThat(me.getBody().email()).isEqualTo(newEmail);
    }

    @Test
    void cvCanBeUploadedReplacedDownloadedAndRemoved() {
        AuthResponse admin = signup(uniqueEmail(), "CV Org");
        BoardSummary board = createBoard(admin.accessToken(), "Board of Directors");
        DirectorSummary director = createDirector(admin.accessToken(), board.id(), "Femi Johnson");

        ResponseEntity<String> wrongType = uploadCv(admin.accessToken(), director.id(), "virus.exe",
                "application/octet-stream", "x".getBytes(StandardCharsets.UTF_8));
        assertThat(wrongType.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ResponseEntity<String> tooLarge = uploadCv(admin.accessToken(), director.id(), "huge.pdf", "application/pdf",
                new byte[11 * 1024 * 1024]);
        assertThat(tooLarge.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        assertThat(uploadCv(admin.accessToken(), director.id(), "femi-cv.pdf", "application/pdf",
                "first version".getBytes(StandardCharsets.UTF_8)).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(uploadCv(admin.accessToken(), director.id(), "femi-cv-2026.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "second version".getBytes(StandardCharsets.UTF_8)).getStatusCode()).isEqualTo(HttpStatus.OK);

        DirectorCvSummary cv = getDetail(admin.accessToken(), director.id()).getBody().cv();
        assertThat(cv.fileName()).isEqualTo("femi-cv-2026.docx");
        assertThat(cv.fileSize()).isEqualTo("second version".length());

        ResponseEntity<byte[]> download = restTemplate.exchange("/api/directors/" + director.id() + "/cv",
                HttpMethod.GET, authedRequest(admin.accessToken()), byte[].class);
        assertThat(download.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(new String(download.getBody(), StandardCharsets.UTF_8)).isEqualTo("second version");

        AuthResponse otherOrg = signup(uniqueEmail(), "Other CV Org");
        assertThat(restTemplate.exchange("/api/directors/" + director.id() + "/cv", HttpMethod.GET,
                authedRequest(otherOrg.accessToken()), String.class).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        ResponseEntity<Void> deleted = restTemplate.exchange("/api/directors/" + director.id() + "/cv",
                HttpMethod.DELETE, authedRequest(admin.accessToken()), Void.class);
        assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(getDetail(admin.accessToken(), director.id()).getBody().cv()).isNull();
    }

    @Test
    void directorsCanBeAssignedToCommitteesWithOneChairEach() {
        AuthResponse admin = signup(uniqueEmail(), "Committee Org");
        BoardSummary board = createBoard(admin.accessToken(), "Board of Directors");
        DirectorSummary ada = createDirector(admin.accessToken(), board.id(), "Ada Obi");
        DirectorSummary bola = createDirector(admin.accessToken(), board.id(), "Bola Ade");

        CommitteeSummary audit = createCommittee(admin.accessToken(), board.id(), "Audit Committee");
        CommitteeSummary risk = createCommittee(admin.accessToken(), board.id(), "Risk Committee");
        ResponseEntity<String> duplicateName = restTemplate.exchange("/api/committees", HttpMethod.POST,
                authedRequest(admin.accessToken(), new CreateCommitteeRequest(board.id(), " audit committee ", null,
                        null)),
                String.class);
        assertThat(duplicateName.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        assignMember(admin.accessToken(), audit.id(), ada.id(), CommitteeMemberRole.CHAIR);
        assignMember(admin.accessToken(), risk.id(), ada.id(), CommitteeMemberRole.MEMBER);
        // Assigning the same director again changes their role rather than adding a second row.
        assignMember(admin.accessToken(), risk.id(), ada.id(), CommitteeMemberRole.MEMBER);

        DirectorDetail adaDetail = getDetail(admin.accessToken(), ada.id()).getBody();
        assertThat(adaDetail.committees()).extracting("committeeName").containsExactly("Audit Committee",
                "Risk Committee");
        assertThat(adaDetail.committees()).extracting("role").containsExactly(CommitteeMemberRole.CHAIR,
                CommitteeMemberRole.MEMBER);

        // A new chair moves the previous chair to member.
        CommitteeSummary afterNewChair = assignMember(admin.accessToken(), audit.id(), bola.id(),
                CommitteeMemberRole.CHAIR);
        assertThat(afterNewChair.members()).hasSize(2);
        assertThat(afterNewChair.members()).filteredOn(m -> m.role() == CommitteeMemberRole.CHAIR)
                .extracting(CommitteeMemberSummary::directorId).containsExactly(bola.id());

        ResponseEntity<CommitteeSummary> removed = restTemplate.exchange(
                "/api/committees/" + risk.id() + "/members/" + ada.id(), HttpMethod.DELETE,
                authedRequest(admin.accessToken()), CommitteeSummary.class);
        assertThat(removed.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(removed.getBody().members()).isEmpty();
        assertThat(getDetail(admin.accessToken(), ada.id()).getBody().committees()).hasSize(1);

        // A director from another organisation can't be put on this committee.
        AuthResponse otherOrg = signup(uniqueEmail(), "Other Committee Org");
        BoardSummary otherBoard = createBoard(otherOrg.accessToken(), "Other Board");
        DirectorSummary outsider = createDirector(otherOrg.accessToken(), otherBoard.id(), "Outsider");
        ResponseEntity<String> crossOrg = restTemplate.exchange("/api/committees/" + audit.id() + "/members",
                HttpMethod.POST, authedRequest(admin.accessToken(), new AddCommitteeMemberRequest(outsider.id(),
                        CommitteeMemberRole.MEMBER)),
                String.class);
        assertThat(crossOrg.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private DirectorSummary createDirectorWithEmail(String token, UUID boardId, String name, String email) {
        ResponseEntity<DirectorSummary> response = restTemplate.exchange("/api/directors", HttpMethod.POST,
                authedRequest(token, directorRequest(boardId, name, email,
                        DirectorClassification.NON_EXECUTIVE_DIRECTOR)),
                DirectorSummary.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody();
    }

    private ResponseEntity<DirectorDetail> getDetail(String token, UUID directorId) {
        return restTemplate.exchange("/api/directors/" + directorId, HttpMethod.GET, authedRequest(token),
                DirectorDetail.class);
    }

    private HttpStatus detailStatus(String token, UUID directorId) {
        return HttpStatus.valueOf(restTemplate.exchange("/api/directors/" + directorId, HttpMethod.GET,
                authedRequest(token), String.class).getStatusCode().value());
    }

    private CommitteeSummary createCommittee(String token, UUID boardId, String name) {
        ResponseEntity<CommitteeSummary> response = restTemplate.exchange("/api/committees", HttpMethod.POST,
                authedRequest(token, new CreateCommitteeRequest(boardId, name, null, null)), CommitteeSummary.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody();
    }

    private List<CommitteeSummary> listCommittees(String token, UUID boardId) {
        return List.of(restTemplate.exchange("/api/committees?boardId=" + boardId, HttpMethod.GET,
                authedRequest(token), CommitteeSummary[].class).getBody());
    }

    private CommitteeSummary assignMember(String token, UUID committeeId, UUID directorId, CommitteeMemberRole role) {
        ResponseEntity<CommitteeSummary> response = restTemplate.exchange(
                "/api/committees/" + committeeId + "/members", HttpMethod.POST,
                authedRequest(token, new AddCommitteeMemberRequest(directorId, role)), CommitteeSummary.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody();
    }

    private ResponseEntity<String> uploadCv(String token, UUID directorId, String fileName, String contentType,
            byte[] bytes) {
        ByteArrayResource fileResource = new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return fileName;
            }
        };
        HttpHeaders filePartHeaders = new HttpHeaders();
        filePartHeaders.setContentType(MediaType.parseMediaType(contentType));
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new HttpEntity<>(fileResource, filePartHeaders));
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return restTemplate.exchange("/api/directors/" + directorId + "/cv", HttpMethod.POST,
                new HttpEntity<>(body, headers), String.class);
    }
}
