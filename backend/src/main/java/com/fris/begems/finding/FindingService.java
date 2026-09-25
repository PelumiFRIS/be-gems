package com.fris.begems.finding;

import com.fris.begems.audit.AuditAction;
import com.fris.begems.audit.AuditEntityType;
import com.fris.begems.audit.AuditLogService;
import com.fris.begems.common.ApiException;
import com.fris.begems.evaluation.Evaluation;
import com.fris.begems.evaluation.EvaluationRepository;
import com.fris.begems.evaluation.EvaluationStatus;
import com.fris.begems.finding.dto.CreateFindingRequest;
import com.fris.begems.finding.dto.FindingSummary;
import com.fris.begems.framework.Dimension;
import com.fris.begems.framework.DimensionRepository;
import com.fris.begems.security.AppUserPrincipal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FindingService {

    private final FindingRepository findingRepository;
    private final EvaluationRepository evaluationRepository;
    private final DimensionRepository dimensionRepository;
    private final AuditLogService auditLogService;

    public FindingService(FindingRepository findingRepository, EvaluationRepository evaluationRepository,
            DimensionRepository dimensionRepository, AuditLogService auditLogService) {
        this.findingRepository = findingRepository;
        this.evaluationRepository = evaluationRepository;
        this.dimensionRepository = dimensionRepository;
        this.auditLogService = auditLogService;
    }

    public List<FindingSummary> listForEvaluation(AppUserPrincipal principal, UUID evaluationId) {
        requireEvaluation(principal, evaluationId);
        List<Finding> findings = findingRepository.findByEvaluationId(evaluationId);

        List<UUID> dimensionIds = findings.stream().map(Finding::getDimensionId).filter(Objects::nonNull)
                .distinct().toList();
        Map<UUID, String> dimensionNamesById = dimensionRepository.findAllById(dimensionIds).stream()
                .collect(Collectors.toMap(Dimension::getId, Dimension::getName));

        return findings.stream()
                .map(finding -> FindingSummary.from(finding, dimensionNamesById.get(finding.getDimensionId())))
                .toList();
    }

    public FindingSummary getOne(AppUserPrincipal principal, UUID findingId) {
        Finding finding = requireFindingInOrganization(principal, findingId);
        return FindingSummary.from(finding, dimensionName(finding.getDimensionId()));
    }

    @Transactional
    public FindingSummary create(AppUserPrincipal principal, UUID evaluationId, CreateFindingRequest request) {
        Evaluation evaluation = requireEvaluation(principal, evaluationId);
        if (evaluation.getStatus() != EvaluationStatus.SCORED) {
            throw ApiException.conflict("Findings can only be added to a scored evaluation");
        }

        Finding finding = Finding.create(principal.getOrganizationId(), evaluationId, request.dimensionId(),
                request.description(), request.severity(), request.evidence(), request.regulatoryReference(),
                request.rootCause(), request.riskImplication());
        findingRepository.save(finding);

        auditLogService.record(principal, AuditAction.FINDING_CREATED, AuditEntityType.FINDING, finding.getId(),
                "Recorded a " + request.severity() + " finding");

        return FindingSummary.from(finding, dimensionName(finding.getDimensionId()));
    }

    @Transactional
    public FindingSummary update(AppUserPrincipal principal, UUID findingId, CreateFindingRequest request) {
        Finding finding = requireFindingInOrganization(principal, findingId);

        finding.setDimensionId(request.dimensionId());
        finding.setDescription(request.description());
        finding.setSeverity(request.severity());
        finding.setEvidence(request.evidence());
        finding.setRegulatoryReference(request.regulatoryReference());
        finding.setRootCause(request.rootCause());
        finding.setRiskImplication(request.riskImplication());
        findingRepository.save(finding);

        return FindingSummary.from(finding, dimensionName(finding.getDimensionId()));
    }

    Finding requireFindingInOrganization(AppUserPrincipal principal, UUID findingId) {
        return findingRepository.findByIdAndOrganizationId(findingId, principal.getOrganizationId())
                .orElseThrow(() -> ApiException.notFound("Finding not found"));
    }

    private Evaluation requireEvaluation(AppUserPrincipal principal, UUID evaluationId) {
        return evaluationRepository.findByIdAndOrganizationId(evaluationId, principal.getOrganizationId())
                .orElseThrow(() -> ApiException.notFound("Evaluation not found"));
    }

    private String dimensionName(UUID dimensionId) {
        if (dimensionId == null) {
            return null;
        }
        return dimensionRepository.findById(dimensionId).map(Dimension::getName).orElse(null);
    }
}
