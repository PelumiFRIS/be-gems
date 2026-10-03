package com.fris.begems.committee;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommitteeMemberRepository extends JpaRepository<CommitteeMember, UUID> {

    List<CommitteeMember> findByCommitteeId(UUID committeeId);

    List<CommitteeMember> findByCommitteeIdIn(Collection<UUID> committeeIds);

    List<CommitteeMember> findByDirectorId(UUID directorId);

    Optional<CommitteeMember> findByCommitteeIdAndDirectorId(UUID committeeId, UUID directorId);

    Optional<CommitteeMember> findByCommitteeIdAndRole(UUID committeeId, CommitteeMemberRole role);

    void deleteByDirectorId(UUID directorId);
}
