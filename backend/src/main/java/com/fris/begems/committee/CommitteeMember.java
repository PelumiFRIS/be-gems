package com.fris.begems.committee;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "committee_members")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CommitteeMember {

    @Id
    private UUID id;

    @Column(name = "committee_id", nullable = false)
    private UUID committeeId;

    @Column(name = "director_id", nullable = false)
    private UUID directorId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CommitteeMemberRole role;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public static CommitteeMember create(UUID committeeId, UUID directorId, CommitteeMemberRole role) {
        CommitteeMember member = new CommitteeMember();
        member.setId(UUID.randomUUID());
        member.setCommitteeId(committeeId);
        member.setDirectorId(directorId);
        member.setRole(role);
        member.setCreatedAt(Instant.now());
        return member;
    }
}
