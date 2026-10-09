package com.fris.begems.skill.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/** A null rating clears the director's rating for the competency. */
public record SkillRatingRequest(@Min(1) @Max(5) Integer rating) {
}
