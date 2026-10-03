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

    @Column(name = "re_election_date")
    private LocalDate reElectionDate;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column
    private String phone;

    @Column
    private String address;

    @Column
    private String profession;

    @Column
    private String qualification;

    @Column
    private String experience;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public static Director create(UUID organizationId, UUID boardId, DirectorProfile profile) {
        Director director = new Director();
        director.setId(UUID.randomUUID());
        director.setOrganizationId(organizationId);
        director.setBoardId(boardId);
        director.setCreatedAt(Instant.now());
        director.applyProfile(profile);
        return director;
    }

    public void applyProfile(DirectorProfile profile) {
        setName(profile.name().trim());
        setEmail(profile.email().trim());
        setClassification(profile.classification());
        setAppointmentDate(profile.appointmentDate());
        setTermExpirationDate(profile.termExpirationDate());
        setReElectionDate(profile.reElectionDate());
        setDateOfBirth(profile.dateOfBirth());
        setPhone(blankToNull(profile.phone()));
        setAddress(blankToNull(profile.address()));
        setProfession(blankToNull(profile.profession()));
        setQualification(blankToNull(profile.qualification()));
        setExperience(blankToNull(profile.experience()));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
