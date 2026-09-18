package com.fris.begems.scoring;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Seeded (V2 migration) — the 6 governance maturity bands from memo section 18. */
@Entity
@Table(name = "maturity_levels")
@Getter
@Setter
@NoArgsConstructor
public class MaturityLevel {

    @Id
    private int level;

    @Column(nullable = false)
    private String label;

    @Column(name = "min_score", nullable = false)
    private BigDecimal minScore;

    @Column(name = "max_score", nullable = false)
    private BigDecimal maxScore;

    @Column(nullable = false)
    private String description;
}
