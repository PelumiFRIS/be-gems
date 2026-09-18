package com.fris.begems.evaluation.dto;

import java.util.List;

public record EvaluationDetail(EvaluationSummary evaluation, List<RespondentSummary> respondents) {
}
