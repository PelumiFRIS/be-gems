package com.fris.begems.report;

import com.fris.begems.framework.Question;
import com.fris.begems.framework.ResponseType;
import com.fris.begems.response.Response;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Question- and dimension-level aggregation for reports. Uses the same rule as
 * ScoringService (a question's score is the mean of its ratings; a dimension's is
 * the mean of its question scores), so a report computed over every respondent
 * agrees with the persisted scores, and the same code can be run over a subset
 * (self, peers, Chairman) for the individual director report.
 */
final class ResponseStatistics {

    private ResponseStatistics() {
    }

    record QuestionResult(Question question, BigDecimal average, int responseCount) {
    }

    /** Rated questions with at least one rating, in questionnaire order. */
    static List<QuestionResult> questionResults(List<Question> questions, Collection<Response> responses) {
        Map<UUID, List<Integer>> ratingsByQuestion = new HashMap<>();
        for (Response response : responses) {
            if (response.getRatingValue() != null) {
                ratingsByQuestion.computeIfAbsent(response.getQuestionId(), k -> new ArrayList<>())
                        .add(response.getRatingValue());
            }
        }
        List<QuestionResult> results = new ArrayList<>();
        for (Question question : questions) {
            List<Integer> ratings = ratingsByQuestion.get(question.getId());
            if (ratings == null || ratings.isEmpty()) {
                continue;
            }
            BigDecimal sum = BigDecimal.valueOf(ratings.stream().mapToInt(Integer::intValue).sum());
            results.add(new QuestionResult(question,
                    sum.divide(BigDecimal.valueOf(ratings.size()), 6, RoundingMode.HALF_UP), ratings.size()));
        }
        return results;
    }

    static Map<UUID, BigDecimal> dimensionAverages(List<QuestionResult> results) {
        Map<UUID, List<BigDecimal>> byDimension = new LinkedHashMap<>();
        for (QuestionResult result : results) {
            byDimension.computeIfAbsent(result.question().getDimensionId(), k -> new ArrayList<>())
                    .add(result.average());
        }
        Map<UUID, BigDecimal> averages = new LinkedHashMap<>();
        byDimension.forEach((dimensionId, scores) -> averages.put(dimensionId, average(scores)));
        return averages;
    }

    static BigDecimal average(Collection<BigDecimal> values) {
        if (values.isEmpty()) {
            return null;
        }
        BigDecimal sum = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(values.size()), 6, RoundingMode.HALF_UP);
    }

    /** Non-blank narrative answers per question, in questionnaire order; never attributed. */
    static Map<Question, List<String>> comments(List<Question> questions, Collection<Response> responses) {
        Map<UUID, List<String>> textByQuestion = new HashMap<>();
        for (Response response : responses) {
            if (response.getTextValue() != null && !response.getTextValue().isBlank()) {
                textByQuestion.computeIfAbsent(response.getQuestionId(), k -> new ArrayList<>())
                        .add(response.getTextValue().trim());
            }
        }
        Map<Question, List<String>> comments = new LinkedHashMap<>();
        for (Question question : questions) {
            List<String> texts = textByQuestion.get(question.getId());
            if (question.getResponseType() == ResponseType.NARRATIVE && texts != null) {
                comments.put(question, texts);
            }
        }
        return comments;
    }
}
