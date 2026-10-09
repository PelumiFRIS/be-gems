package com.fris.begems.skill.dto;

import com.fris.begems.skill.RequiredLevel;
import com.fris.begems.skill.SkillCoverage;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** {@code ratings} is keyed by director id; unrated directors are absent. */
public record SkillRow(UUID id, String name, RequiredLevel requiredLevel, boolean futureFocus, int displayOrder,
        Map<UUID, Integer> ratings, Double average, int proficientCount, List<String> proficientDirectors,
        SkillCoverage coverage) {
}
