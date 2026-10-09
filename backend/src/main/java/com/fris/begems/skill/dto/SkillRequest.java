package com.fris.begems.skill.dto;

import com.fris.begems.skill.RequiredLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SkillRequest(@NotBlank @Size(max = 120) String name, @NotNull RequiredLevel requiredLevel,
        boolean futureFocus) {
}
