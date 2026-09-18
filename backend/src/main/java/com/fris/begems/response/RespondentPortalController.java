package com.fris.begems.response;

import com.fris.begems.response.dto.MyEvaluationSummary;
import com.fris.begems.response.dto.QuestionWithAnswer;
import com.fris.begems.response.dto.SaveResponseRequest;
import com.fris.begems.security.AppUserPrincipal;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/my-evaluations")
public class RespondentPortalController {

    private final ResponseService responseService;

    public RespondentPortalController(ResponseService responseService) {
        this.responseService = responseService;
    }

    @GetMapping
    public List<MyEvaluationSummary> list(@AuthenticationPrincipal AppUserPrincipal principal) {
        return responseService.listMyEvaluations(principal);
    }

    @GetMapping("/{evaluationId}/questions")
    public List<QuestionWithAnswer> getQuestions(@AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable UUID evaluationId) {
        return responseService.getQuestions(principal, evaluationId);
    }

    @PutMapping("/{evaluationId}/responses/{questionId}")
    public QuestionWithAnswer saveResponse(@AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable UUID evaluationId, @PathVariable UUID questionId,
            @Valid @RequestBody SaveResponseRequest request) {
        return responseService.saveResponse(principal, evaluationId, questionId, request);
    }

    @PostMapping("/{evaluationId}/submit")
    public void submit(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable UUID evaluationId) {
        responseService.submit(principal, evaluationId);
    }
}
