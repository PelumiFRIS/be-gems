package com.fris.begems.committee.dto;

import com.fris.begems.committee.CommitteeMemberRole;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AddCommitteeMemberRequest(@NotNull UUID directorId, @NotNull CommitteeMemberRole role) {
}
