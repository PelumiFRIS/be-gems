package com.fris.begems.approval;

import com.fris.begems.approval.dto.ApprovalEventSummary;
import com.fris.begems.approval.dto.ApprovalRequest;
import com.fris.begems.approval.dto.MyBoardReport;
import com.fris.begems.approval.dto.ReportApprovalStatus;
import com.fris.begems.approval.dto.StageSummary;
import com.fris.begems.audit.AuditAction;
import com.fris.begems.audit.AuditEntityType;
import com.fris.begems.audit.AuditLogService;
import com.fris.begems.board.Board;
import com.fris.begems.board.BoardRepository;
import com.fris.begems.common.ApiException;
import com.fris.begems.director.Director;
import com.fris.begems.director.DirectorClassification;
import com.fris.begems.director.DirectorRepository;
import com.fris.begems.evaluation.Evaluation;
import com.fris.begems.evaluation.EvaluationRepository;
import com.fris.begems.framework.EvaluationType;
import com.fris.begems.notification.NotificationService;
import com.fris.begems.report.ReportService;
import com.fris.begems.security.AppUserPrincipal;
import com.fris.begems.user.Role;
import com.fris.begems.user.User;
import com.fris.begems.user.UserRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Memo §32: Evaluator Review → Draft Report → Quality Review → Company Secretary Review → Chairman/Board Approval
 * → Final Report. Board evaluations only. A report can be returned for changes with a comment, and the final
 * version is stored when the last approval is given.
 */
@Service
public class ReportApprovalService {

    private final EvaluationRepository evaluationRepository;
    private final ReportApprovalEventRepository eventRepository;
    private final BoardReportAccess access;
    private final ReportService reportService;
    private final UserRepository userRepository;
    private final DirectorRepository directorRepository;
    private final BoardRepository boardRepository;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;

    public ReportApprovalService(EvaluationRepository evaluationRepository,
            ReportApprovalEventRepository eventRepository, BoardReportAccess access, ReportService reportService,
            UserRepository userRepository, DirectorRepository directorRepository, BoardRepository boardRepository,
            NotificationService notificationService, AuditLogService auditLogService) {
        this.evaluationRepository = evaluationRepository;
        this.eventRepository = eventRepository;
        this.access = access;
        this.reportService = reportService;
        this.userRepository = userRepository;
        this.directorRepository = directorRepository;
        this.boardRepository = boardRepository;
        this.notificationService = notificationService;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public ReportApprovalStatus status(AppUserPrincipal principal, UUID evaluationId) {
        Evaluation evaluation = evaluationRepository
                .findByIdAndOrganizationId(evaluationId, principal.getOrganizationId())
                .orElseThrow(() -> ApiException.notFound("Evaluation not found"));
        requireViewable(principal, evaluation);
        return toStatus(principal, evaluation);
    }

    @Transactional
    public ReportApprovalStatus approve(AppUserPrincipal principal, UUID evaluationId, ApprovalRequest request) {
        Evaluation evaluation = lock(principal, evaluationId);
        requireViewable(principal, evaluation);
        ReportStage from = evaluation.getReportStage();
        if (from == ReportStage.FINAL) {
            throw ApiException.conflict("The report has already been approved");
        }
        if (!canAct(principal, evaluation)) {
            throw ApiException.forbidden(whoActs(from));
        }
        String comment = trimToNull(request == null ? null : request.comment());
        if (comment == null && commentRequired(principal, evaluation)) {
            throw ApiException.badRequest(
                    "Record the Board's approval, for example the date of the meeting where it was given");
        }

        ReportStage to = from.next();
        String actorName = record(principal, evaluation, from, to, ApprovalDecision.APPROVED, comment);
        if (to == ReportStage.FINAL) {
            reportService.issueFinal(evaluation);
            auditLogService.record(principal, AuditAction.REPORT_FINALISED, AuditEntityType.EVALUATION,
                    evaluation.getId(), "Approved the " + evaluation.getYear() + " board evaluation report as final");
            notificationService.notifyReportFinalised(evaluation, excluding(finalRecipients(evaluation), principal));
        } else {
            auditLogService.record(principal, AuditAction.REPORT_STAGE_APPROVED, AuditEntityType.EVALUATION,
                    evaluation.getId(), "Approved the board evaluation report at " + from.label()
                            + "; it moved to " + to.label());
            notificationService.notifyReportAwaitingApproval(evaluation, to.label(), actorName,
                    excluding(recipientsFor(evaluation, to), principal));
        }
        return toStatus(principal, evaluation);
    }

    @Transactional
    public ReportApprovalStatus returnForChanges(AppUserPrincipal principal, UUID evaluationId,
            ApprovalRequest request) {
        Evaluation evaluation = lock(principal, evaluationId);
        requireViewable(principal, evaluation);
        ReportStage from = evaluation.getReportStage();
        if (from == ReportStage.FINAL) {
            throw ApiException.conflict("The report has already been approved");
        }
        if (from == ReportStage.EVALUATOR_REVIEW) {
            throw ApiException.conflict("The report is already at the first stage");
        }
        if (!canAct(principal, evaluation)) {
            throw ApiException.forbidden(whoActs(from));
        }
        String comment = trimToNull(request == null ? null : request.comment());
        if (comment == null) {
            throw ApiException.badRequest("Explain what needs to change before returning the report");
        }

        ReportStage to = from.returnTarget();
        String actorName = record(principal, evaluation, from, to, ApprovalDecision.RETURNED, comment);
        auditLogService.record(principal, AuditAction.REPORT_RETURNED, AuditEntityType.EVALUATION,
                evaluation.getId(), "Returned the board evaluation report from " + from.label() + " to " + to.label());
        notificationService.notifyReportReturned(evaluation, to.label(), actorName, comment,
                excluding(notificationService.evaluationStaff(evaluation.getOrganizationId()), principal));
        return toStatus(principal, evaluation);
    }

    @Transactional(readOnly = true)
    public List<MyBoardReport> myBoardReports(AppUserPrincipal principal) {
        if (principal.getRole() != Role.DIRECTOR) {
            return List.of();
        }
        return directorRepository.findByUserId(principal.getUserId())
                .filter(d -> d.getOrganizationId().equals(principal.getOrganizationId()))
                .map(seat -> {
                    String boardName = boardRepository.findById(seat.getBoardId()).map(Board::getName).orElse("");
                    return evaluationRepository.findByBoardId(seat.getBoardId()).stream()
                            .filter(e -> e.getEvaluationType() == EvaluationType.BOARD)
                            .filter(e -> access.canView(principal, e))
                            .sorted(Comparator.comparingInt(Evaluation::getYear).reversed())
                            .map(e -> new MyBoardReport(e.getId(), e.getYear(), boardName, e.getReportStage(),
                                    e.getReportStage().label(), canAct(principal, e)))
                            .toList();
                })
                .orElse(List.of());
    }

    private Evaluation lock(AppUserPrincipal principal, UUID evaluationId) {
        return evaluationRepository.lockByIdAndOrganizationId(evaluationId, principal.getOrganizationId())
                .orElseThrow(() -> ApiException.notFound("Evaluation not found"));
    }

    private void requireViewable(AppUserPrincipal principal, Evaluation evaluation) {
        if (evaluation.getEvaluationType() != EvaluationType.BOARD) {
            throw ApiException.conflict("Only Board Evaluation Reports go through approval");
        }
        if (evaluation.getReportStage() == null) {
            throw ApiException.conflict("The report enters approval once scores have been calculated");
        }
        if (!access.canView(principal, evaluation)) {
            throw ApiException.forbidden("The Board Evaluation Report is shared with directors once it is approved");
        }
    }

    private String record(AppUserPrincipal principal, Evaluation evaluation, ReportStage from, ReportStage to,
            ApprovalDecision decision, String comment) {
        String actorName = userRepository.findById(principal.getUserId())
                .map(u -> (u.getFirstName() + " " + u.getLastName()).trim())
                .orElse("Unknown user");
        eventRepository.save(ReportApprovalEvent.create(evaluation.getOrganizationId(), evaluation.getId(), from, to,
                decision, comment, principal.getUserId(), actorName));
        evaluation.setReportStage(to);
        evaluationRepository.save(evaluation);
        return actorName;
    }

    private boolean canAct(AppUserPrincipal principal, Evaluation evaluation) {
        return switch (evaluation.getReportStage()) {
            case EVALUATOR_REVIEW, DRAFT_REPORT, QUALITY_REVIEW ->
                    principal.actsAs(Role.COMPANY_SECRETARY) || principal.actsAs(Role.EVALUATOR);
            case CS_REVIEW -> principal.actsAs(Role.COMPANY_SECRETARY);
            case BOARD_APPROVAL -> access.isChairman(principal, evaluation)
                    || principal.actsAs(Role.COMPANY_SECRETARY);
            case FINAL -> false;
        };
    }

    /** The Company Secretary may record the Board's approval, but must say when and where it was given. */
    private boolean commentRequired(AppUserPrincipal principal, Evaluation evaluation) {
        return evaluation.getReportStage() == ReportStage.BOARD_APPROVAL && !access.isChairman(principal, evaluation);
    }

    private static String whoActs(ReportStage stage) {
        return switch (stage) {
            case EVALUATOR_REVIEW, DRAFT_REPORT, QUALITY_REVIEW ->
                    "This stage is completed by an Evaluator or the Company Secretary";
            case CS_REVIEW -> "This stage is completed by the Company Secretary";
            case BOARD_APPROVAL -> "This stage is completed by the Chairman, or by the Company Secretary on the "
                    + "Board's behalf";
            case FINAL -> "The report has already been approved";
        };
    }

    private String approveLabel(AppUserPrincipal principal, Evaluation evaluation) {
        return switch (evaluation.getReportStage()) {
            case EVALUATOR_REVIEW -> "Results reviewed — prepare draft report";
            case DRAFT_REPORT -> "Send draft for quality review";
            case QUALITY_REVIEW -> "Quality review passed — send to Company Secretary";
            case CS_REVIEW -> "Send to the Chairman/Board for approval";
            case BOARD_APPROVAL -> access.isChairman(principal, evaluation)
                    ? "Approve the final report" : "Record the Board's approval";
            case FINAL -> null;
        };
    }

    private ReportApprovalStatus toStatus(AppUserPrincipal principal, Evaluation evaluation) {
        ReportStage stage = evaluation.getReportStage();
        boolean canAct = canAct(principal, evaluation);
        List<ApprovalEventSummary> history = eventRepository
                .findByEvaluationIdOrderByCreatedAtAsc(evaluation.getId()).stream()
                .map(ApprovalEventSummary::from)
                .toList();
        return new ReportApprovalStatus(evaluation.getId(), evaluation.getYear(), stage, stage.label(),
                StageSummary.all(), history, canAct, canAct && stage != ReportStage.EVALUATOR_REVIEW,
                canAct && commentRequired(principal, evaluation), canAct ? approveLabel(principal, evaluation) : null);
    }

    // ----- Recipients -----

    private List<User> recipientsFor(Evaluation evaluation, ReportStage stage) {
        UUID orgId = evaluation.getOrganizationId();
        return switch (stage) {
            case EVALUATOR_REVIEW, DRAFT_REPORT, QUALITY_REVIEW -> notificationService.evaluationStaff(orgId);
            case CS_REVIEW -> companySecretaries(orgId);
            case BOARD_APPROVAL -> {
                List<User> users = new ArrayList<>(companySecretaries(orgId));
                users.addAll(boardUsers(evaluation, true));
                yield users;
            }
            case FINAL -> finalRecipients(evaluation);
        };
    }

    private List<User> finalRecipients(Evaluation evaluation) {
        List<User> users = new ArrayList<>(userRepository.findByOrganizationId(evaluation.getOrganizationId())
                .stream()
                .filter(u -> u.actsAs(Role.ORG_ADMIN) || u.actsAs(Role.COMPANY_SECRETARY)
                        || u.actsAs(Role.EVALUATOR))
                .toList());
        users.addAll(boardUsers(evaluation, false));
        return users;
    }

    private List<User> companySecretaries(UUID organizationId) {
        return userRepository.findByOrganizationId(organizationId).stream()
                .filter(u -> u.actsAs(Role.COMPANY_SECRETARY))
                .toList();
    }

    private List<User> boardUsers(Evaluation evaluation, boolean chairmanOnly) {
        List<UUID> userIds = directorRepository.findByBoardId(evaluation.getBoardId()).stream()
                .filter(d -> !chairmanOnly || d.getClassification() == DirectorClassification.CHAIRMAN)
                .map(Director::getUserId)
                .filter(Objects::nonNull)
                .toList();
        return userRepository.findAllById(userIds);
    }

    private static List<User> excluding(List<User> users, AppUserPrincipal actor) {
        Map<UUID, User> unique = new LinkedHashMap<>();
        users.stream()
                .filter(u -> !u.getId().equals(actor.getUserId()))
                .forEach(u -> unique.putIfAbsent(u.getId(), u));
        return List.copyOf(unique.values());
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
