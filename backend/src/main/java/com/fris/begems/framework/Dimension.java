package com.fris.begems.framework;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Seeded (the 18 dimensions from the memo, NCCG 2018) — weights fixed for MVP, see plan. */
@Entity
@Table(name = "dimensions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Dimension {

    @Id
    private UUID id;

    @Column(name = "framework_id", nullable = false)
    private UUID frameworkId;

    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "default_weight_pct", nullable = false)
    private BigDecimal defaultWeightPct;

    @Column(name = "bgei_category")
    private String bgeiCategory;
}
