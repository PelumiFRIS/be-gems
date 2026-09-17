package com.fris.begems.auth.dto;

import com.fris.begems.user.dto.UserSummary;

public record AuthResponse(String accessToken, UserSummary user) {
}
