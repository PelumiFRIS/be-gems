package com.fris.begems.committee;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommitteeMemberRepository extends JpaRepository<CommitteeMember, UUID> {

    List<CommitteeMember> findByCommitteeId(UUID committeeId);
}
