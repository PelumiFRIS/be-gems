package com.fris.begems.scoring;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Seeded (V2 migration) — the 6 Board Governance Effectiveness Index bands from memo section 40. */
@Entity
@Table(name = "bgei_bands")
@Getter
@Setter
@NoArgsConstructor
public class BgeiBand {

    @Id
    @Column(name = "min_pct")
    private BigDecimal minPct;

    @Column(name = "max_pct", nullable = false)
    private BigDecimal maxPct;

    @Column(nullable = false)
    private String label;
}
