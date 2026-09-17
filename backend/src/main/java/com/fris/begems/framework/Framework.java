package com.fris.begems.framework;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Seeded reference data (NCCG 2018 for MVP) — no write endpoints yet; more frameworks are Phase 2. */
@Entity
@Table(name = "frameworks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Framework {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String version;

    @Column(name = "effective_date")
    private LocalDate effectiveDate;

    @Column(name = "is_active", nullable = false)
    private boolean active;
}
