package com.fris.begems.skill;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "director_skill_ratings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DirectorSkillRating {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "director_id", nullable = false)
    private UUID directorId;

    @Column(name = "skill_id", nullable = false)
    private UUID skillId;

    @Column(nullable = false)
    private int rating;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static DirectorSkillRating create(UUID organizationId, UUID directorId, UUID skillId, int rating) {
        DirectorSkillRating entry = new DirectorSkillRating();
        entry.setId(UUID.randomUUID());
        entry.setOrganizationId(organizationId);
        entry.setDirectorId(directorId);
        entry.setSkillId(skillId);
        entry.setRating(rating);
        entry.setUpdatedAt(Instant.now());
        return entry;
    }
}
