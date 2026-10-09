package com.fris.begems.approval;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportApprovalEventRepository extends JpaRepository<ReportApprovalEvent, UUID> {

    List<ReportApprovalEvent> findByEvaluationIdOrderByCreatedAtAsc(UUID evaluationId);
}
