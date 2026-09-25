package com.fris.begems.recommendation;

import com.fris.begems.audit.AuditAction;
import com.fris.begems.audit.AuditEntityType;
import com.fris.begems.audit.AuditLogService;
import com.fris.begems.common.ApiException;
import com.fris.begems.finding.Finding;
import com.fris.begems.finding.FindingRepository;
import com.fris.begems.recommendation.dto.CreateRecommendationRequest;
import com.fris.begems.recommendation.dto.RecommendationSummary;
import com.fris.begems.security.AppUserPrincipal;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecommendationService {

    private final RecommendationRepository recommendationRepository;
    private final FindingRepository findingRepository;
    private final AuditLogService auditLogService;

    public RecommendationService(RecommendationRepository recommendationRepository,
            FindingRepository findingRepository, AuditLogService auditLogService) {
        this.recommendationRepository = recommendationRepository;
        this.findingRepository = findingRepository;
        this.auditLogService = auditLogService;
    }

    public List<RecommendationSummary> listForFinding(AppUserPrincipal principal, UUID findingId) {
        requireFinding(principal, findingId);
        return recommendationRepository.findByFindingId(findingId).stream()
                .map(RecommendationSummary::from)
                .toList();
    }

    @Transactional
    public RecommendationSummary create(AppUserPrincipal principal, UUID findingId,
            CreateRecommendationRequest request) {
        requireFinding(principal, findingId);

        Recommendation recommendation = Recommendation.create(principal.getOrganizationId(), findingId,
                request.recommendedAction(), request.responsiblePerson(), request.committeeResponsible(),
                request.targetDate(), request.priority(), request.status());
        recommendationRepository.save(recommendation);

        auditLogService.record(principal, AuditAction.RECOMMENDATION_CREATED, AuditEntityType.RECOMMENDATION,
                recommendation.getId(), "Added a recommendation");

        return RecommendationSummary.from(recommendation);
    }

    @Transactional
    public RecommendationSummary update(AppUserPrincipal principal, UUID recommendationId,
            CreateRecommendationRequest request) {
        Recommendation recommendation = recommendationRepository
                .findByIdAndOrganizationId(recommendationId, principal.getOrganizationId())
                .orElseThrow(() -> ApiException.notFound("Recommendation not found"));

        recommendation.setRecommendedAction(request.recommendedAction());
        recommendation.setResponsiblePerson(request.responsiblePerson());
        recommendation.setCommitteeResponsible(request.committeeResponsible());
        recommendation.setTargetDate(request.targetDate());
        recommendation.setPriority(request.priority());
        recommendation.setStatus(request.status());
        recommendationRepository.save(recommendation);

        return RecommendationSummary.from(recommendation);
    }

    private Finding requireFinding(AppUserPrincipal principal, UUID findingId) {
        return findingRepository.findByIdAndOrganizationId(findingId, principal.getOrganizationId())
                .orElseThrow(() -> ApiException.notFound("Finding not found"));
    }
}
