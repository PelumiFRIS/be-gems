package com.fris.begems.report;

import com.fris.begems.action.CorrectiveAction;
import com.fris.begems.action.CorrectiveActionRepository;
import com.fris.begems.audit.AuditAction;
import com.fris.begems.audit.AuditEntityType;
import com.fris.begems.audit.AuditLogService;
import com.fris.begems.board.Board;
import com.fris.begems.board.BoardRepository;
import com.fris.begems.committee.Committee;
import com.fris.begems.committee.CommitteeMember;
import com.fris.begems.committee.CommitteeMemberRepository;
import com.fris.begems.committee.CommitteeRepository;
import com.fris.begems.common.ApiException;
import com.fris.begems.director.Director;
import com.fris.begems.director.DirectorRepository;
import com.fris.begems.evaluation.Evaluation;
import com.fris.begems.evaluation.EvaluationRepository;
import com.fris.begems.evaluation.EvaluationRespondent;
import com.fris.begems.evaluation.EvaluationRespondentRepository;
import com.fris.begems.evaluation.EvaluationStatus;
import com.fris.begems.finding.Finding;
import com.fris.begems.finding.FindingRepository;
import com.fris.begems.framework.Dimension;
import com.fris.begems.framework.DimensionRepository;
import com.fris.begems.framework.EvaluationType;
import com.fris.begems.framework.Framework;
import com.fris.begems.framework.FrameworkRepository;
import com.fris.begems.framework.Question;
import com.fris.begems.framework.QuestionRepository;
import com.fris.begems.organization.Organization;
import com.fris.begems.organization.OrganizationRepository;
import com.fris.begems.recommendation.Recommendation;
import com.fris.begems.recommendation.RecommendationRepository;
import com.fris.begems.response.Response;
import com.fris.begems.response.ResponseRepository;
import com.fris.begems.scoring.BgeiBandRepository;
import com.fris.begems.scoring.EvaluationScore;
import com.fris.begems.scoring.EvaluationScoreRepository;
import com.fris.begems.scoring.MaturityLevelRepository;
import com.fris.begems.scoring.ScoreScopeType;
import com.fris.begems.security.AppUserPrincipal;
import com.fris.begems.user.Role;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Board reports are open to ORG_ADMIN, COMPANY_SECRETARY and EVALUATOR (the
 * controller's @PreAuthorize); individual director reports (memo §27, "strictly
 * access-controlled") additionally exclude ORG_ADMIN, whose role is technical
 * administration. Every generation is audit-logged.
 */
@Service
public class ReportService {

    private final EvaluationRepository evaluationRepository;
    private final EvaluationRespondentRepository respondentRepository;
    private final ResponseRepository responseRepository;
    private final QuestionRepository questionRepository;
    private final DimensionRepository dimensionRepository;
    private final FrameworkRepository frameworkRepository;
    private final EvaluationScoreRepository evaluationScoreRepository;
    private final MaturityLevelRepository maturityLevelRepository;
    private final BgeiBandRepository bgeiBandRepository;
    private final OrganizationRepository organizationRepository;
    private final BoardRepository boardRepository;
    private final DirectorRepository directorRepository;
    private final CommitteeRepository committeeRepository;
    private final CommitteeMemberRepository committeeMemberRepository;
    private final FindingRepository findingRepository;
    private final RecommendationRepository recommendationRepository;
    private final CorrectiveActionRepository correctiveActionRepository;
    private final BoardReportWriter boardReportWriter;
    private final DirectorReportWriter directorReportWriter;
    private final AuditLogService auditLogService;

    public ReportService(EvaluationRepository evaluationRepository,
            EvaluationRespondentRepository respondentRepository, ResponseRepository responseRepository,
            QuestionRepository questionRepository, DimensionRepository dimensionRepository,
            FrameworkRepository frameworkRepository, EvaluationScoreRepository evaluationScoreRepository,
            MaturityLevelRepository maturityLevelRepository, BgeiBandRepository bgeiBandRepository,
            OrganizationRepository organizationRepository, BoardRepository boardRepository,
            DirectorRepository directorRepository, CommitteeRepository committeeRepository,
            CommitteeMemberRepository committeeMemberRepository, FindingRepository findingRepository,
            RecommendationRepository recommendationRepository,
            CorrectiveActionRepository correctiveActionRepository, BoardReportWriter boardReportWriter,
            DirectorReportWriter directorReportWriter, AuditLogService auditLogService) {
        this.evaluationRepository = evaluationRepository;
        this.respondentRepository = respondentRepository;
        this.responseRepository = responseRepository;
        this.questionRepository = questionRepository;
        this.dimensionRepository = dimensionRepository;
        this.frameworkRepository = frameworkRepository;
        this.evaluationScoreRepository = evaluationScoreRepository;
        this.maturityLevelRepository = maturityLevelRepository;
        this.bgeiBandRepository = bgeiBandRepository;
        this.organizationRepository = organizationRepository;
        this.boardRepository = boardRepository;
        this.directorRepository = directorRepository;
        this.committeeRepository = committeeRepository;
        this.committeeMemberRepository = committeeMemberRepository;
        this.findingRepository = findingRepository;
        this.recommendationRepository = recommendationRepository;
        this.correctiveActionRepository = correctiveActionRepository;
        this.boardReportWriter = boardReportWriter;
        this.directorReportWriter = directorReportWriter;
        this.auditLogService = auditLogService;
    }

    public record GeneratedReport(String fileName, byte[] html) {
    }

    @Transactional
    public GeneratedReport generate(AppUserPrincipal principal, UUID evaluationId) {
        Evaluation evaluation = evaluationRepository
                .findByIdAndOrganizationId(evaluationId, principal.getOrganizationId())
                .orElseThrow(() -> ApiException.notFound("Evaluation not found"));
        boolean individual = evaluation.getEvaluationType() == EvaluationType.DIRECTOR_PEER;
        if (individual && principal.getRole() == Role.ORG_ADMIN) {
            throw ApiException.forbidden(
                    "Individual director reports are restricted to the Company Secretary and Evaluators");
        }
        if (evaluation.getStatus() != EvaluationStatus.SCORED) {
            throw ApiException.conflict("The report is available once scores have been calculated");
        }

        ReportContext ctx = loadContext(evaluation);
        byte[] html;
        String fileName;
        String summary;
        if (individual) {
            String directorName = ctx.directors().stream()
                    .filter(d -> d.getId().equals(evaluation.getSubjectDirectorId()))
                    .map(Director::getName).findFirst().orElse("Director");
            html = directorReportWriter.write(ctx);
            fileName = directorName + " - Individual Director Report " + evaluation.getYear() + ".html";
            summary = "Generated confidential individual director report for " + directorName;
        } else {
            html = boardReportWriter.write(ctx);
            fileName = ctx.organization().getName() + " - Board Evaluation Report " + evaluation.getYear() + ".html";
            summary = "Generated board evaluation report";
        }

        auditLogService.record(principal, AuditAction.REPORT_GENERATED, AuditEntityType.EVALUATION, evaluationId,
                summary);
        return new GeneratedReport(fileName.replaceAll("[\\\\/:*?\"<>|]", ""), html);
    }

    private ReportContext loadContext(Evaluation evaluation) {
        Organization organization = organizationRepository.findById(evaluation.getOrganizationId())
                .orElseThrow(() -> ApiException.notFound("Organisation not found"));
        Board board = boardRepository.findByIdAndOrganizationId(evaluation.getBoardId(), evaluation.getOrganizationId())
                .orElseThrow(() -> ApiException.notFound("Board not found"));
        Framework framework = frameworkRepository.findById(evaluation.getFrameworkId())
                .orElseThrow(() -> ApiException.notFound("Framework not found"));

        List<Committee> committees = committeeRepository.findByBoardId(board.getId());
        List<CommitteeMember> committeeMembers = committees.isEmpty() ? List.of()
                : committeeMemberRepository.findByCommitteeIdIn(committees.stream().map(Committee::getId).toList());

        List<EvaluationRespondent> respondents = respondentRepository.findByEvaluationId(evaluation.getId());
        List<Response> responses = respondents.isEmpty() ? List.of()
                : responseRepository.findByEvaluationRespondentIdIn(
                        respondents.stream().map(EvaluationRespondent::getId).toList());

        List<Finding> findings = findingRepository.findByEvaluationId(evaluation.getId());
        List<UUID> findingIds = findings.stream().map(Finding::getId).toList();
        List<Recommendation> recommendations = findingIds.isEmpty() ? List.of()
                : recommendationRepository.findByFindingIdIn(findingIds);
        List<CorrectiveAction> actions = findingIds.isEmpty() ? List.of()
                : correctiveActionRepository.findByFindingIdIn(findingIds);

        List<Dimension> dimensions = dimensionRepository.findByFrameworkIdOrderByDisplayOrder(framework.getId());
        Map<UUID, Integer> dimensionOrder = dimensions.stream()
                .collect(Collectors.toMap(Dimension::getId, Dimension::getDisplayOrder));
        // Question display order restarts within each dimension, so group by dimension first.
        List<Question> questions = questionRepository
                .findByFrameworkIdAndEvaluationTypeOrderByDisplayOrder(framework.getId(), evaluation.getEvaluationType())
                .stream()
                .sorted(Comparator.comparing((Question q) -> dimensionOrder.getOrDefault(q.getDimensionId(),
                                Integer.MAX_VALUE))
                        .thenComparingInt(Question::getDisplayOrder))
                .toList();

        return new ReportContext(
                organization,
                board,
                framework,
                evaluation,
                directorRepository.findByBoardId(board.getId()),
                committees,
                committeeMembers,
                dimensions,
                evaluationScoreRepository.findByEvaluationId(evaluation.getId()),
                maturityLevelRepository.findAll(),
                bgeiBandRepository.findAll(),
                questions,
                respondents,
                responses,
                findings,
                recommendations,
                actions,
                evaluation.getEvaluationType() == EvaluationType.BOARD ? peerDirectorScores(evaluation) : List.of(),
                LocalDate.now());
    }

    /** Overall scores of the same board's scored peer evaluations for the same year — reported only in aggregate. */
    private List<BigDecimal> peerDirectorScores(Evaluation boardEvaluation) {
        return evaluationRepository.findByBoardId(boardEvaluation.getBoardId()).stream()
                .filter(e -> e.getEvaluationType() == EvaluationType.DIRECTOR_PEER
                        && e.getYear() == boardEvaluation.getYear()
                        && e.getStatus() == EvaluationStatus.SCORED)
                .map(e -> evaluationScoreRepository.findByEvaluationId(e.getId()).stream()
                        .filter(s -> s.getScopeType() == ScoreScopeType.DIRECTOR_OVERALL)
                        .map(EvaluationScore::getRawScore)
                        .findFirst().orElse(null))
                .filter(Objects::nonNull)
                .toList();
    }
}
