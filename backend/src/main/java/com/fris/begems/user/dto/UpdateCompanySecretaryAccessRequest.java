package com.fris.begems.user.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateCompanySecretaryAccessRequest(@NotNull Boolean enabled) {
}
