package com.fris.begems.user.dto;

import com.fris.begems.user.UserStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateUserStatusRequest(@NotNull UserStatus status) {
}
