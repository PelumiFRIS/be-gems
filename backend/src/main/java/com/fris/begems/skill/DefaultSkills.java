package com.fris.begems.skill;

import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

/**
 * The memo §8 competency list, with Board Requirement levels taken from the memo's
 * example matrix where it gives one. V9__board_skills_matrix.sql backfills the same
 * list for boards that existed before this module, so keep the two in step.
 */
public final class DefaultSkills {

    private record Default(String name, RequiredLevel requiredLevel, boolean futureFocus) {
    }

    private static final List<Default> DEFAULTS = List.of(
            new Default("Strategy", RequiredLevel.HIGH, false),
            new Default("Finance", RequiredLevel.HIGH, false),
            new Default("Accounting", RequiredLevel.MEDIUM, false),
            new Default("Audit", RequiredLevel.HIGH, false),
            new Default("Risk", RequiredLevel.HIGH, false),
            new Default("Banking", RequiredLevel.MEDIUM, false),
            new Default("Legal", RequiredLevel.MEDIUM, false),
            new Default("Regulatory", RequiredLevel.MEDIUM, false),
            new Default("Technology", RequiredLevel.HIGH, true),
            new Default("Cybersecurity", RequiredLevel.MEDIUM, true),
            new Default("Digital transformation", RequiredLevel.MEDIUM, true),
            new Default("Human resources", RequiredLevel.LOW, false),
            new Default("ESG", RequiredLevel.MEDIUM, true),
            new Default("International business", RequiredLevel.LOW, false),
            new Default("Industry knowledge", RequiredLevel.HIGH, false),
            new Default("Capital markets", RequiredLevel.MEDIUM, false),
            new Default("Marketing", RequiredLevel.LOW, false),
            new Default("Corporate governance", RequiredLevel.HIGH, false));

    private DefaultSkills() {
    }

    public static List<BoardSkill> forBoard(UUID organizationId, UUID boardId) {
        return IntStream.range(0, DEFAULTS.size())
                .mapToObj(i -> BoardSkill.create(organizationId, boardId, DEFAULTS.get(i).name(),
                        DEFAULTS.get(i).requiredLevel(), DEFAULTS.get(i).futureFocus(), i + 1))
                .toList();
    }
}
