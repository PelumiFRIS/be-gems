package com.fris.begems.recommendation;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecommendationRepository extends JpaRepository<Recommendation, UUID> {

    List<Recommendation> findByFindingId(UUID findingId);

    List<Recommendation> findByFindingIdIn(Collection<UUID> findingIds);

    Optional<Recommendation> findByIdAndOrganizationId(UUID id, UUID organizationId);
}
