package com.fris.begems.skill;

import com.fris.begems.director.Director;
import com.fris.begems.director.DirectorClassification;
import com.fris.begems.skill.dto.MatrixDirector;
import com.fris.begems.skill.dto.SkillRow;
import com.fris.begems.skill.dto.SkillsMatrix;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Classifies each competency against its Board Requirement. Proficient means a
 * rating of 4 (Advanced) or 5 (Expert); competent means 3 or above.
 * <ul>
 * <li>HIGH needs two proficient directors: none is a critical gap, exactly one is a
 * single-person dependency.</li>
 * <li>MEDIUM needs one proficient director, otherwise it's underrepresented.</li>
 * <li>LOW needs one competent director, otherwise it's underrepresented.</li>
 * <li>A competency nobody has been rated on yet is not assessed.</li>
 * </ul>
 */
public final class SkillsAnalysis {

    public static final int PROFICIENT = 4;
    public static final int COMPETENT = 3;

    private SkillsAnalysis() {
    }

    public static SkillsMatrix build(UUID boardId, List<Director> directors, List<BoardSkill> skills,
            List<DirectorSkillRating> ratings) {
        List<Director> ordered = directors.stream()
                .sorted(Comparator.comparing((Director d) -> d.getClassification() == DirectorClassification.CHAIRMAN
                                ? 0 : 1)
                        .thenComparing(Director::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
        Map<UUID, Director> directorsById = ordered.stream()
                .collect(Collectors.toMap(Director::getId, Function.identity()));
        Map<UUID, List<DirectorSkillRating>> ratingsBySkill = ratings.stream()
                .filter(r -> directorsById.containsKey(r.getDirectorId()))
                .collect(Collectors.groupingBy(DirectorSkillRating::getSkillId));

        List<SkillRow> rows = skills.stream()
                .map(skill -> row(skill, ratingsBySkill.getOrDefault(skill.getId(), List.of()), directorsById))
                .toList();

        Map<SkillCoverage, Long> counts = new EnumMap<>(SkillCoverage.class);
        for (SkillCoverage coverage : SkillCoverage.values()) {
            counts.put(coverage, rows.stream().filter(r -> r.coverage() == coverage).count());
        }
        return new SkillsMatrix(boardId, ordered.stream().map(MatrixDirector::from).toList(), rows, counts);
    }

    public static SkillCoverage classify(RequiredLevel requiredLevel, List<Integer> ratings) {
        if (ratings.isEmpty()) {
            return SkillCoverage.NOT_ASSESSED;
        }
        long proficient = ratings.stream().filter(r -> r >= PROFICIENT).count();
        long competent = ratings.stream().filter(r -> r >= COMPETENT).count();
        return switch (requiredLevel) {
            case HIGH -> proficient == 0 ? SkillCoverage.CRITICAL_GAP
                    : proficient == 1 ? SkillCoverage.SINGLE_PERSON_DEPENDENCY : SkillCoverage.COVERED;
            case MEDIUM -> proficient == 0 ? SkillCoverage.UNDERREPRESENTED : SkillCoverage.COVERED;
            case LOW -> competent == 0 ? SkillCoverage.UNDERREPRESENTED : SkillCoverage.COVERED;
        };
    }

    private static SkillRow row(BoardSkill skill, List<DirectorSkillRating> skillRatings,
            Map<UUID, Director> directorsById) {
        Map<UUID, Integer> byDirector = new LinkedHashMap<>();
        skillRatings.forEach(r -> byDirector.put(r.getDirectorId(), r.getRating()));
        List<Integer> values = List.copyOf(byDirector.values());
        Double average = values.isEmpty() ? null
                : Math.round(values.stream().mapToInt(Integer::intValue).average().orElse(0) * 100) / 100.0;
        List<String> proficientDirectors = skillRatings.stream()
                .filter(r -> r.getRating() >= PROFICIENT)
                .sorted(Comparator.comparingInt(DirectorSkillRating::getRating).reversed())
                .map(r -> directorsById.get(r.getDirectorId()).getName())
                .toList();
        return new SkillRow(skill.getId(), skill.getName(), skill.getRequiredLevel(), skill.isFutureFocus(),
                skill.getDisplayOrder(), byDirector, average, proficientDirectors.size(), proficientDirectors,
                classify(skill.getRequiredLevel(), values));
    }
}
