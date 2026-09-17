package com.fris.begems.framework.dto;

import com.fris.begems.framework.Question;
import com.fris.begems.framework.ResponseType;
import java.util.UUID;

public record QuestionSummary(UUID id, UUID dimensionId, String text, ResponseType responseType, boolean mandatory) {

    public static QuestionSummary from(Question question) {
        return new QuestionSummary(question.getId(), question.getDimensionId(), question.getText(),
                question.getResponseType(), question.isMandatory());
    }
}
