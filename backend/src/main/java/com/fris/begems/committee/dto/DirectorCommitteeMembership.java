package com.fris.begems.committee.dto;

import com.fris.begems.committee.CommitteeMemberRole;
import java.util.UUID;

public record DirectorCommitteeMembership(UUID committeeId, String committeeName, CommitteeMemberRole role) {
}
