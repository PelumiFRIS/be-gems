package com.fris.begems.board;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** One active board per organisation in the MVP (memo's multi-board support is Phase 2+). */
@Entity
@Table(name = "boards")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Board {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(nullable = false)
    private String name;

    @Column(name = "effective_date")
    private LocalDate effectiveDate;

    @Column
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public static Board create(UUID organizationId, String name, LocalDate effectiveDate, String notes) {
        Board board = new Board();
        board.setId(UUID.randomUUID());
        board.setOrganizationId(organizationId);
        board.setName(name);
        board.setEffectiveDate(effectiveDate);
        board.setNotes(notes);
        board.setCreatedAt(Instant.now());
        return board;
    }
}
