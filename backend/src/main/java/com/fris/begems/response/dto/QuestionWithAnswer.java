package com.fris.begems.response.dto;

import com.fris.begems.framework.Question;
import com.fris.begems.framework.ResponseType;
import com.fris.begems.response.Response;
import java.math.BigDecimal;
import java.util.UUID;

public record QuestionWithAnswer(
        UUID questionId,
        UUID dimensionId,
        String text,
        ResponseType responseType,
        boolean mandatory,
        Integer ratingValue,
        String textValue,
        BigDecimal numericValue) {

    public static QuestionWithAnswer from(Question question, Response answer) {
        return new QuestionWithAnswer(
                question.getId(),
                question.getDimensionId(),
                question.getText(),
                question.getResponseType(),
                question.isMandatory(),
                answer == null ? null : answer.getRatingValue(),
                answer == null ? null : answer.getTextValue(),
                answer == null ? null : answer.getNumericValue());
    }
}
