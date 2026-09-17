package com.fris.begems.committee.dto;

import com.fris.begems.committee.Committee;
import java.util.List;
import java.util.UUID;

public record CommitteeSummary(
        UUID id,
        UUID boardId,
        String name,
        String meetingFrequency,
        String mandate,
        List<CommitteeMemberSummary> members) {

    public static CommitteeSummary from(Committee committee, List<CommitteeMemberSummary> members) {
        return new CommitteeSummary(committee.getId(), committee.getBoardId(), committee.getName(),
                committee.getMeetingFrequency(), committee.getMandate(), members);
    }
}
