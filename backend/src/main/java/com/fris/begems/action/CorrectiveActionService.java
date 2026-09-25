package com.fris.begems.action;

import com.fris.begems.action.dto.ActionRegisterRow;
import com.fris.begems.action.dto.ActionSummary;
import com.fris.begems.action.dto.CreateActionRequest;
import com.fris.begems.action.dto.UpdateActionRequest;
import com.fris.begems.audit.AuditAction;
import com.fris.begems.audit.AuditEntityType;
import com.fris.begems.audit.AuditLogService;
import com.fris.begems.common.ApiException;
import com.fris.begems.evaluation.Evaluation;
import com.fris.begems.evaluation.EvaluationRepository;
import com.fris.begems.finding.Finding;
import com.fris.begems.finding.FindingRepository;
import com.fris.begems.security.AppUserPrincipal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CorrectiveActionService {

    private final CorrectiveActionRepository actionRepository;
    private final FindingRepository findingRepository;
    private final EvaluationRepository evaluationRepository;
    private final AuditLogService auditLogService;

    public CorrectiveActionService(CorrectiveActionRepository actionRepository, FindingRepository findingRepository,
            EvaluationRepository evaluationRepository, AuditLogService auditLogService) {
        this.actionRepository = actionRepository;
        this.findingRepository = findingRepository;
        this.evaluationRepository = evaluationRepository;
        this.auditLogService = auditLogService;
    }

    public List<ActionSummary> listForFinding(AppUserPrincipal principal, UUID findingId) {
        requireFinding(principal, findingId);
        return actionRepository.findByFindingId(findingId).stream().map(ActionSummary::from).toList();
    }

    public List<ActionRegisterRow> listRegister(AppUserPrincipal principal) {
        List<CorrectiveAction> actions = actionRepository.findByOrganizationId(principal.getOrganizationId());
        if (actions.isEmpty()) {
            return List.of();
        }

        List<UUID> findingIds = actions.stream().map(CorrectiveAction::getFindingId).distinct().toList();
        Map<UUID, Finding> findingsById = findingRepository.findAllById(findingIds).stream()
                .collect(Collectors.toMap(Finding::getId, f -> f));

        List<UUID> evaluationIds = findingsById.values().stream().map(Finding::getEvaluationId).distinct().toList();
        Map<UUID, Evaluation> evaluationsById = evaluationRepository.findAllById(evaluationIds).stream()
                .collect(Collectors.toMap(Evaluation::getId, e -> e));

        return actions.stream()
                .map(action -> {
                    Finding finding = findingsById.get(action.getFindingId());
                    Evaluation evaluation = finding == null ? null : evaluationsById.get(finding.getEvaluationId());
                    return ActionRegisterRow.from(action, evaluation == null ? null : evaluation.getId(),
                            evaluation == null ? 0 : evaluation.getYear(),
                            evaluation == null ? null : evaluation.getEvaluationType(),
                            finding == null ? null : finding.getDescription(),
                            finding == null ? null : finding.getSeverity());
                })
                .toList();
    }

    @Transactional
    public ActionSummary create(AppUserPrincipal principal, UUID findingId, CreateActionRequest request) {
        requireFinding(principal, findingId);

        CorrectiveAction action = CorrectiveAction.create(principal.getOrganizationId(), findingId,
                request.description(), request.owner(), request.approver(), request.dueDate(), request.evidence());
        actionRepository.save(action);

        auditLogService.record(principal, AuditAction.ACTION_CREATED, AuditEntityType.ACTION, action.getId(),
                "Added a corrective action");

        return ActionSummary.from(action);
    }

    @Transactional
    public ActionSummary update(AppUserPrincipal principal, UUID actionId, UpdateActionRequest request) {
        CorrectiveAction action = actionRepository
                .findByIdAndOrganizationId(actionId, principal.getOrganizationId())
                .orElseThrow(() -> ApiException.notFound("Action not found"));

        boolean newlyCompleted = request.status() == ActionStatus.COMPLETED
                && action.getStatus() != ActionStatus.COMPLETED;

        action.setDescription(request.description());
        action.setOwner(request.owner());
        action.setApprover(request.approver());
        action.setDueDate(request.dueDate());
        action.setStatus(request.status());
        action.setEvidence(request.evidence());
        if (newlyCompleted && action.getClosureDate() == null) {
            action.setClosureDate(LocalDate.now());
        }
        actionRepository.save(action);

        if (newlyCompleted) {
            auditLogService.record(principal, AuditAction.ACTION_CLOSED, AuditEntityType.ACTION, action.getId(),
                    "Closed a corrective action");
        }

        return ActionSummary.from(action);
    }

    private Finding requireFinding(AppUserPrincipal principal, UUID findingId) {
        return findingRepository.findByIdAndOrganizationId(findingId, principal.getOrganizationId())
                .orElseThrow(() -> ApiException.notFound("Finding not found"));
    }
}
