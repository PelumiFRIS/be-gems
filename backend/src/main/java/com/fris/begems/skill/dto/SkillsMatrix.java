package com.fris.begems.skill.dto;

import com.fris.begems.skill.SkillCoverage;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record SkillsMatrix(UUID boardId, List<MatrixDirector> directors, List<SkillRow> skills,
        Map<SkillCoverage, Long> coverageCounts) {
}
