package com.fris.begems.response;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * ratingValue (1-5) is used for RATING_1_5, and also for YES_NO (5/1) and
 * YES_NO_PARTIALLY (5/3/1) — the mapping described in the scoring plan is applied
 * at submission time rather than at scoring time, which is simpler and loses no
 * information (the question's responseType tells you which mapping applied).
 * textValue is for NARRATIVE; numericValue is for PERCENTAGE/NUMERIC
 * (informational only — excluded from scoring).
 */
@Entity
@Table(name = "responses")
@Getter
@Setter
@NoArgsConstructor
public class Response {

    @Id
    private UUID id;

    @Column(name = "evaluation_respondent_id", nullable = false)
    private UUID evaluationRespondentId;

    @Column(name = "question_id", nullable = false)
    private UUID questionId;

    @Column(name = "rating_value")
    private Integer ratingValue;

    @Column(name = "text_value")
    private String textValue;

    @Column(name = "numeric_value")
    private BigDecimal numericValue;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static Response create(UUID evaluationRespondentId, UUID questionId) {
        Response response = new Response();
        response.setId(UUID.randomUUID());
        response.setEvaluationRespondentId(evaluationRespondentId);
        response.setQuestionId(questionId);
        response.setUpdatedAt(Instant.now());
        return response;
    }
}
