package com.fris.begems.user.dto;

import com.fris.begems.user.Role;
import jakarta.validation.constraints.NotNull;

public record UpdateUserRoleRequest(@NotNull Role role) {
}
