package com.fris.begems.framework;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Seeded from the memo's two example questionnaires — data, not hardcoded logic. */
@Entity
@Table(name = "questions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Question {

    @Id
    private UUID id;

    @Column(name = "framework_id", nullable = false)
    private UUID frameworkId;

    @Column(name = "dimension_id", nullable = false)
    private UUID dimensionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "evaluation_type", nullable = false)
    private EvaluationType evaluationType;

    @Column(nullable = false)
    private String text;

    @Enumerated(EnumType.STRING)
    @Column(name = "response_type", nullable = false)
    private ResponseType responseType;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "is_mandatory", nullable = false)
    private boolean mandatory;
}
