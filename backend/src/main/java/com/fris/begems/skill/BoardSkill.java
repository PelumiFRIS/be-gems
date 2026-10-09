package com.fris.begems.skill;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "board_skills")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BoardSkill {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "board_id", nullable = false)
    private UUID boardId;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "required_level", nullable = false)
    private RequiredLevel requiredLevel;

    @Column(name = "future_focus", nullable = false)
    private boolean futureFocus;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public static BoardSkill create(UUID organizationId, UUID boardId, String name, RequiredLevel requiredLevel,
            boolean futureFocus, int displayOrder) {
        BoardSkill skill = new BoardSkill();
        skill.setId(UUID.randomUUID());
        skill.setOrganizationId(organizationId);
        skill.setBoardId(boardId);
        skill.setName(name);
        skill.setRequiredLevel(requiredLevel);
        skill.setFutureFocus(futureFocus);
        skill.setDisplayOrder(displayOrder);
        skill.setCreatedAt(Instant.now());
        return skill;
    }
}
