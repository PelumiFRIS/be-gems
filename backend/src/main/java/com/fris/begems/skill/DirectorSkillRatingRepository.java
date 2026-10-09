package com.fris.begems.skill;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DirectorSkillRatingRepository extends JpaRepository<DirectorSkillRating, UUID> {

    List<DirectorSkillRating> findBySkillIdIn(Collection<UUID> skillIds);

    Optional<DirectorSkillRating> findByDirectorIdAndSkillId(UUID directorId, UUID skillId);

    void deleteBySkillId(UUID skillId);
}
