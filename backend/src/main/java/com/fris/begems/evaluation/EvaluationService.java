package com.fris.begems.evaluation;

import com.fris.begems.audit.AuditAction;
import com.fris.begems.audit.AuditEntityType;
import com.fris.begems.audit.AuditLogService;
import com.fris.begems.board.BoardRepository;
import com.fris.begems.common.ApiException;
import com.fris.begems.director.Director;
import com.fris.begems.director.DirectorRepository;
import com.fris.begems.evaluation.dto.AddRespondentRequest;
import com.fris.begems.evaluation.dto.CreateEvaluationRequest;
import com.fris.begems.evaluation.dto.EvaluationDetail;
import com.fris.begems.evaluation.dto.EvaluationSummary;
import com.fris.begems.evaluation.dto.RespondentSummary;
import com.fris.begems.framework.EvaluationType;
import com.fris.begems.framework.Framework;
import com.fris.begems.framework.FrameworkRepository;
import com.fris.begems.security.AppUserPrincipal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EvaluationService {

    private static final String ACTIVE_FRAMEWORK_CODE = "NCCG_2018";

    private final EvaluationRepository evaluationRepository;
    private final EvaluationRespondentRepository respondentRepository;
    private final BoardRepository boardRepository;
    private final DirectorRepository directorRepository;
    private final FrameworkRepository frameworkRepository;
    private final AuditLogService auditLogService;

    public EvaluationService(EvaluationRepository evaluationRepository,
            EvaluationRespondentRepository respondentRepository, BoardRepository boardRepository,
            DirectorRepository directorRepository, FrameworkRepository frameworkRepository,
            AuditLogService auditLogService) {
        this.evaluationRepository = evaluationRepository;
        this.respondentRepository = respondentRepository;
        this.boardRepository = boardRepository;
        this.directorRepository = directorRepository;
        this.frameworkRepository = frameworkRepository;
        this.auditLogService = auditLogService;
    }

    public List<EvaluationSummary> listForBoard(AppUserPrincipal principal, UUID boardId) {
        requireBoardInOrganization(principal, boardId);
        List<Evaluation> evaluations = evaluationRepository.findByBoardId(boardId);
        Map<UUID, String> directorNamesById = directorNamesByIdFor(evaluations.stream()
                .map(Evaluation::getSubjectDirectorId));
        return evaluations.stream()
                .map(evaluation -> EvaluationSummary.from(evaluation,
                        directorNamesById.get(evaluation.getSubjectDirectorId())))
                .toList();
    }

    public EvaluationDetail getDetail(AppUserPrincipal principal, UUID evaluationId) {
        Evaluation evaluation = requireEvaluationInOrganization(principal, evaluationId);
        List<EvaluationRespondent> evaluationRespondents = respondentRepository.findByEvaluationId(evaluationId);

        Map<UUID, String> directorNamesById = directorNamesByIdFor(Stream.concat(
                evaluationRespondents.stream().map(EvaluationRespondent::getDirectorId),
                Stream.of(evaluation.getSubjectDirectorId())));

        List<RespondentSummary> respondents = evaluationRespondents.stream()
                .map(r -> RespondentSummary.from(r, directorNamesById.get(r.getDirectorId())))
                .toList();
        return new EvaluationDetail(
                EvaluationSummary.from(evaluation, directorNamesById.get(evaluation.getSubjectDirectorId())),
                respondents);
    }

    private Map<UUID, String> directorNamesByIdFor(Stream<UUID> directorIds) {
        List<UUID> ids = directorIds.filter(Objects::nonNull).distinct().toList();
        return directorRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Director::getId, Director::getName));
    }

    @Transactional
    public EvaluationSummary create(AppUserPrincipal principal, CreateEvaluationRequest request) {
        requireBoardInOrganization(principal, request.boardId());

        if (request.evaluationType() == EvaluationType.DIRECTOR_PEER && request.subjectDirectorId() == null) {
            throw ApiException.badRequest("A Director Peer-to-Peer evaluation needs a subject director");
        }
        if (request.evaluationType() == EvaluationType.BOARD && request.subjectDirectorId() != null) {
            throw ApiException.badRequest("A Board evaluation doesn't take a subject director");
        }
        if (request.subjectDirectorId() != null) {
            requireDirectorOnBoard(principal, request.subjectDirectorId(), request.boardId());
        }

        Framework framework = frameworkRepository.findByCodeAndActiveTrue(ACTIVE_FRAMEWORK_CODE)
                .orElseThrow(() -> ApiException.notFound("No active governance framework is configured"));

        Evaluation evaluation = Evaluation.create(principal.getOrganizationId(), request.boardId(),
                framework.getId(), request.evaluationType(), request.subjectDirectorId(), request.year());
        evaluationRepository.save(evaluation);

        auditLogService.record(principal, AuditAction.EVALUATION_CREATED, AuditEntityType.EVALUATION,
                evaluation.getId(),
                "Created a " + request.evaluationType() + " evaluation for " + request.year());

        return EvaluationSummary.from(evaluation, subjectDirectorName(evaluation));
    }

    @Transactional
    public EvaluationDetail addRespondent(AppUserPrincipal principal, UUID evaluationId,
            AddRespondentRequest request) {
        Evaluation evaluation = requireEvaluationInOrganization(principal, evaluationId);
        if (evaluation.getStatus() != EvaluationStatus.DRAFT) {
            throw ApiException.conflict("Respondents can only be added while the evaluation is in draft");
        }
        requireDirectorOnBoard(principal, request.directorId(), evaluation.getBoardId());
        if (respondentRepository.existsByEvaluationIdAndDirectorId(evaluationId, request.directorId())) {
            throw ApiException.conflict("This director is already a respondent on this evaluation");
        }

        respondentRepository.save(
                EvaluationRespondent.create(evaluationId, request.directorId(), request.confidentialityMode()));

        return getDetail(principal, evaluationId);
    }

    @Transactional
    public EvaluationSummary launch(AppUserPrincipal principal, UUID evaluationId) {
        Evaluation evaluation = requireEvaluationInOrganization(principal, evaluationId);
        if (evaluation.getStatus() != EvaluationStatus.DRAFT) {
            throw ApiException.conflict("Only a draft evaluation can be launched");
        }
        List<EvaluationRespondent> respondents = respondentRepository.findByEvaluationId(evaluationId);
        if (respondents.isEmpty()) {
            throw ApiException.badRequest("Add at least one respondent before launching");
        }

        evaluation.setStatus(EvaluationStatus.LAUNCHED);
        evaluation.setStartDate(LocalDate.now());
        evaluationRepository.save(evaluation);

        auditLogService.record(principal, AuditAction.EVALUATION_LAUNCHED, AuditEntityType.EVALUATION,
                evaluation.getId(), "Launched the evaluation to " + respondents.size() + " respondent(s)");

        return EvaluationSummary.from(evaluation, subjectDirectorName(evaluation));
    }

    @Transactional
    public EvaluationSummary close(AppUserPrincipal principal, UUID evaluationId) {
        Evaluation evaluation = requireEvaluationInOrganization(principal, evaluationId);
        if (evaluation.getStatus() != EvaluationStatus.LAUNCHED) {
            throw ApiException.conflict("Only a launched evaluation can be closed");
        }

        Instant now = Instant.now();
        for (EvaluationRespondent respondent : respondentRepository.findByEvaluationId(evaluationId)) {
            if (respondent.getStatus() != RespondentStatus.SUBMITTED) {
                respondent.setStatus(RespondentStatus.SUBMITTED);
                respondent.setSubmittedAt(now);
                respondentRepository.save(respondent);
            }
        }

        evaluation.setStatus(EvaluationStatus.CLOSED);
        evaluation.setCloseDate(LocalDate.now());
        evaluationRepository.save(evaluation);

        auditLogService.record(principal, AuditAction.EVALUATION_CLOSED, AuditEntityType.EVALUATION,
                evaluation.getId(), "Closed the evaluation");

        return EvaluationSummary.from(evaluation, subjectDirectorName(evaluation));
    }

    Evaluation requireEvaluationInOrganization(AppUserPrincipal principal, UUID evaluationId) {
        return evaluationRepository.findByIdAndOrganizationId(evaluationId, principal.getOrganizationId())
                .orElseThrow(() -> ApiException.notFound("Evaluation not found"));
    }

    private void requireBoardInOrganization(AppUserPrincipal principal, UUID boardId) {
        boardRepository.findByIdAndOrganizationId(boardId, principal.getOrganizationId())
                .orElseThrow(() -> ApiException.notFound("Board not found"));
    }

    private void requireDirectorOnBoard(AppUserPrincipal principal, UUID directorId, UUID boardId) {
        Director director = directorRepository.findByIdAndOrganizationId(directorId, principal.getOrganizationId())
                .orElseThrow(() -> ApiException.notFound("Director not found"));
        if (!director.getBoardId().equals(boardId)) {
            throw ApiException.badRequest("That director is not on this board");
        }
    }

    private String subjectDirectorName(Evaluation evaluation) {
        if (evaluation.getSubjectDirectorId() == null) {
            return null;
        }
        return directorName(evaluation.getSubjectDirectorId());
    }

    private String directorName(UUID directorId) {
        return directorRepository.findById(directorId).map(Director::getName).orElse(null);
    }
}
