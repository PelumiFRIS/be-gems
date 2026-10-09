package com.fris.begems.approval;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FinalReportRepository extends JpaRepository<FinalReport, UUID> {
}
