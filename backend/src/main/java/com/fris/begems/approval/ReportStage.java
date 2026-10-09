package com.fris.begems.approval;

/** Memo §32, in order. */
public enum ReportStage {
    EVALUATOR_REVIEW("Evaluator Review"),
    DRAFT_REPORT("Draft Report"),
    QUALITY_REVIEW("Quality Review"),
    CS_REVIEW("Company Secretary Review"),
    BOARD_APPROVAL("Chairman/Board Approval"),
    FINAL("Final Report");

    private final String label;

    ReportStage(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public ReportStage next() {
        return this == FINAL ? FINAL : values()[ordinal() + 1];
    }

    /** Returned reports go back to the draft for changes; a draft goes back to the evaluator's review. */
    public ReportStage returnTarget() {
        return switch (this) {
            case EVALUATOR_REVIEW, FINAL -> this;
            case DRAFT_REPORT -> EVALUATOR_REVIEW;
            default -> DRAFT_REPORT;
        };
    }
}
