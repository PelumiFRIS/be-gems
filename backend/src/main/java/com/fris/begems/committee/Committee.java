package com.fris.begems.committee;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "committees")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Committee {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "board_id", nullable = false)
    private UUID boardId;

    @Column(nullable = false)
    private String name;

    @Column(name = "meeting_frequency")
    private String meetingFrequency;

    @Column(name = "mandate")
    private String mandate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public static Committee create(UUID organizationId, UUID boardId, String name, String meetingFrequency,
            String mandate) {
        Committee committee = new Committee();
        committee.setId(UUID.randomUUID());
        committee.setOrganizationId(organizationId);
        committee.setBoardId(boardId);
        committee.setName(name);
        committee.setMeetingFrequency(meetingFrequency);
        committee.setMandate(mandate);
        committee.setCreatedAt(Instant.now());
        return committee;
    }
}
