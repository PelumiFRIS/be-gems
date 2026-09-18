package com.fris.begems.evaluation;

/**
 * IDENTIFIED and CONFIDENTIAL are fully enforced (see EvaluationService/
 * ScoringService's respondent-level access checks). ANONYMOUS is selectable and
 * stored but currently behaves like CONFIDENTIAL — true unlinkability needs a
 * token-based redesign of response submission, deferred to a later phase.
 */
public enum ConfidentialityMode {
    IDENTIFIED,
    CONFIDENTIAL,
    ANONYMOUS
}
