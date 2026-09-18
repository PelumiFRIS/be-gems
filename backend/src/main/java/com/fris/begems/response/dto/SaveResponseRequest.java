package com.fris.begems.response.dto;

import java.math.BigDecimal;

/**
 * Exactly one field is meaningful per question, matching its responseType:
 * ratingValue for RATING_1_5/YES_NO/YES_NO_PARTIALLY (already mapped to 1-5 by the
 * client — see Response.java), textValue for NARRATIVE, numericValue for
 * PERCENTAGE/NUMERIC. Validated against the question's actual type in ResponseService.
 */
public record SaveResponseRequest(Integer ratingValue, String textValue, BigDecimal numericValue) {
}
