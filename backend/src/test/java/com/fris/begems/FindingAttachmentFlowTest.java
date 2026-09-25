package com.fris.begems;

import static org.assertj.core.api.Assertions.assertThat;

import com.fris.begems.attachment.dto.AttachmentSummary;
import com.fris.begems.auth.dto.AuthResponse;
import com.fris.begems.board.dto.BoardSummary;
import com.fris.begems.director.dto.DirectorSummary;
import com.fris.begems.evaluation.ConfidentialityMode;
import com.fris.begems.evaluation.dto.AddRespondentRequest;
import com.fris.begems.evaluation.dto.CreateEvaluationRequest;
import com.fris.begems.evaluation.dto.EvaluationDetail;
import com.fris.begems.evaluation.dto.EvaluationSummary;
import com.fris.begems.finding.FindingSeverity;
import com.fris.begems.finding.dto.CreateFindingRequest;
import com.fris.begems.finding.dto.FindingSummary;
import com.fris.begems.framework.EvaluationType;
import com.fris.begems.framework.dto.QuestionSummary;
import com.fris.begems.response.dto.SaveResponseRequest;
import com.fris.begems.support.IntegrationTestSupport;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * Drives a Board evaluation to SCORED, records a finding, then walks evidence
 * attachment upload/list/download/delete, including the 10MB size cap, role
 * gating and cross-org isolation.
 */
class FindingAttachmentFlowTest extends IntegrationTestSupport {

    @Test
    void evidenceAttachmentFlowOnAFinding() {
        AuthResponse admin = signup(uniqueEmail(), "Attachments Org");
        AuthResponse cs = createCompanySecretaryAndLogin(admin.accessToken());
        BoardSummary board = createBoard(admin.accessToken(), "Board of Directors");
        DirectorSummary director = createDirector(admin.accessToken(), board.id(), "Ada Lovelace");
        AuthResponse directorLogin = inviteDirectorAndLogin(cs.accessToken(), director);

        UUID evaluationId = scoreABoardEvaluation(cs, board, director, directorLogin);

        ResponseEntity<FindingSummary> findingResponse = restTemplate.exchange(
                "/api/evaluations/" + evaluationId + "/findings", HttpMethod.POST,
                authedRequest(cs.accessToken(), new CreateFindingRequest(null, "Minutes were not signed off",
                        FindingSeverity.MEDIUM, null, null, null, null)),
                FindingSummary.class);
        UUID findingId = findingResponse.getBody().id();

        ResponseEntity<AttachmentSummary> uploadResponse = uploadFile(cs.accessToken(), findingId,
                "board-minutes.pdf", "application/pdf", "sample evidence bytes".getBytes(StandardCharsets.UTF_8));
        assertThat(uploadResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        AttachmentSummary uploaded = uploadResponse.getBody();
        assertThat(uploaded.fileName()).isEqualTo("board-minutes.pdf");
        assertThat(uploaded.fileSize()).isEqualTo("sample evidence bytes".getBytes(StandardCharsets.UTF_8).length);
        assertThat(uploaded.uploadedByName()).isEqualTo("Board Secretary");

        ResponseEntity<AttachmentSummary[]> listResponse = restTemplate.exchange(
                "/api/findings/" + findingId + "/attachments", HttpMethod.GET, authedRequest(admin.accessToken()),
                AttachmentSummary[].class);
        assertThat(listResponse.getBody()).hasSize(1);

        ResponseEntity<byte[]> downloadResponse = restTemplate.exchange(
                "/api/attachments/" + uploaded.id() + "/download", HttpMethod.GET, authedRequest(admin.accessToken()),
                byte[].class);
        assertThat(downloadResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(new String(downloadResponse.getBody(), StandardCharsets.UTF_8)).isEqualTo("sample evidence bytes");

        // A file over the 10MB cap is rejected.
        ResponseEntity<String> tooLarge = uploadFileRaw(cs.accessToken(), findingId, "huge.bin",
                "application/octet-stream", new byte[11 * 1024 * 1024]);
        assertThat(tooLarge.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        // A plain director cannot upload evidence.
        ResponseEntity<String> directorAttempt = uploadFileRaw(directorLogin.accessToken(), findingId, "x.txt",
                "text/plain", "x".getBytes(StandardCharsets.UTF_8));
        assertThat(directorAttempt.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        // A different organisation cannot see or download this attachment.
        AuthResponse otherOrgAdmin = signup(uniqueEmail(), "Other Org");
        ResponseEntity<String> crossOrgList = restTemplate.exchange(
                "/api/findings/" + findingId + "/attachments", HttpMethod.GET,
                authedRequest(otherOrgAdmin.accessToken()), String.class);
        assertThat(crossOrgList.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        ResponseEntity<String> crossOrgDownload = restTemplate.exchange(
                "/api/attachments/" + uploaded.id() + "/download", HttpMethod.GET,
                authedRequest(otherOrgAdmin.accessToken()), String.class);
        assertThat(crossOrgDownload.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        ResponseEntity<Void> deleteResponse = restTemplate.exchange(
                "/api/attachments/" + uploaded.id(), HttpMethod.DELETE, authedRequest(cs.accessToken()), Void.class);
        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<AttachmentSummary[]> listAfterDelete = restTemplate.exchange(
                "/api/findings/" + findingId + "/attachments", HttpMethod.GET, authedRequest(admin.accessToken()),
                AttachmentSummary[].class);
        assertThat(listAfterDelete.getBody()).isEmpty();
    }

    private ResponseEntity<AttachmentSummary> uploadFile(String token, UUID findingId, String fileName,
            String contentType, byte[] bytes) {
        HttpEntity<MultiValueMap<String, Object>> request = multipartRequest(token, fileName, contentType, bytes);
        return restTemplate.exchange("/api/findings/" + findingId + "/attachments", HttpMethod.POST, request,
                AttachmentSummary.class);
    }

    private ResponseEntity<String> uploadFileRaw(String token, UUID findingId, String fileName, String contentType,
            byte[] bytes) {
        HttpEntity<MultiValueMap<String, Object>> request = multipartRequest(token, fileName, contentType, bytes);
        return restTemplate.exchange("/api/findings/" + findingId + "/attachments", HttpMethod.POST, request,
                String.class);
    }

    private HttpEntity<MultiValueMap<String, Object>> multipartRequest(String token, String fileName,
            String contentType, byte[] bytes) {
        ByteArrayResource fileResource = new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return fileName;
            }
        };
        HttpHeaders filePartHeaders = new HttpHeaders();
        filePartHeaders.setContentType(org.springframework.http.MediaType.parseMediaType(contentType));

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new HttpEntity<>(fileResource, filePartHeaders));

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return new HttpEntity<>(body, headers);
    }

    private UUID scoreABoardEvaluation(AuthResponse cs, BoardSummary board, DirectorSummary director,
            AuthResponse directorLogin) {
        ResponseEntity<EvaluationSummary> created = restTemplate.exchange(
                "/api/evaluations", HttpMethod.POST,
                authedRequest(cs.accessToken(), new CreateEvaluationRequest(board.id(), EvaluationType.BOARD, null, 2026)),
                EvaluationSummary.class);
        UUID evaluationId = created.getBody().id();

        restTemplate.exchange("/api/evaluations/" + evaluationId + "/respondents", HttpMethod.POST,
                authedRequest(cs.accessToken(), new AddRespondentRequest(director.id(), ConfidentialityMode.CONFIDENTIAL)),
                EvaluationDetail.class);

        restTemplate.exchange("/api/evaluations/" + evaluationId + "/launch", HttpMethod.POST,
                authedRequest(cs.accessToken()), EvaluationSummary.class);

        ResponseEntity<QuestionSummary[]> questions = restTemplate.exchange(
                "/api/frameworks/active/questions?evaluationType=BOARD", HttpMethod.GET,
                authedRequest(cs.accessToken()), QuestionSummary[].class);
        for (QuestionSummary question : questions.getBody()) {
            restTemplate.exchange("/api/my-evaluations/" + evaluationId + "/responses/" + question.id(),
                    HttpMethod.PUT, authedRequest(directorLogin.accessToken(), new SaveResponseRequest(4, null, null)),
                    String.class);
        }
        restTemplate.exchange("/api/my-evaluations/" + evaluationId + "/submit", HttpMethod.POST,
                authedRequest(directorLogin.accessToken()), Void.class);

        restTemplate.exchange("/api/evaluations/" + evaluationId + "/close", HttpMethod.POST,
                authedRequest(cs.accessToken()), EvaluationSummary.class);

        restTemplate.exchange("/api/evaluations/" + evaluationId + "/scores/calculate", HttpMethod.POST,
                authedRequest(cs.accessToken()), String.class);

        return evaluationId;
    }
}
