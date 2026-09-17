package com.fris.begems.committee.dto;

import com.fris.begems.committee.CommitteeMember;
import com.fris.begems.committee.CommitteeMemberRole;
import java.util.UUID;

public record CommitteeMemberSummary(UUID directorId, CommitteeMemberRole role) {

    public static CommitteeMemberSummary from(CommitteeMember member) {
        return new CommitteeMemberSummary(member.getDirectorId(), member.getRole());
    }
}
