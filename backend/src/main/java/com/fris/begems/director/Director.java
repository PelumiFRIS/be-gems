package com.fris.begems.director;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Also the questionnaire respondent once an evaluation is launched. */
@Entity
@Table(name = "directors")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Director {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "board_id", nullable = false)
    private UUID boardId;

    @Column(nullable = false)
    private String name;

    @Column
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DirectorClassification classification;

    @Column(name = "appointment_date")
    private LocalDate appointmentDate;

    @Column(name = "term_expiration_date")
    private LocalDate termExpirationDate;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public static Director create(UUID organizationId, UUID boardId, String name, String email,
            DirectorClassification classification, LocalDate appointmentDate, LocalDate termExpirationDate) {
        Director director = new Director();
        director.setId(UUID.randomUUID());
        director.setOrganizationId(organizationId);
        director.setBoardId(boardId);
        director.setName(name);
        director.setEmail(email);
        director.setClassification(classification);
        director.setAppointmentDate(appointmentDate);
        director.setTermExpirationDate(termExpirationDate);
        director.setCreatedAt(Instant.now());
        return director;
    }
}
