-- Memo §32: a scored Board evaluation's report moves through Evaluator Review →
-- Draft Report → Quality Review → Company Secretary Review → Chairman/Board
-- Approval → Final Report. NULL until the evaluation is scored, and for peer
-- evaluations, whose confidential reports don't go through this workflow.
ALTER TABLE evaluations ADD COLUMN report_stage VARCHAR(30);

UPDATE evaluations SET report_stage = 'EVALUATOR_REVIEW'
WHERE status = 'SCORED' AND evaluation_type = 'BOARD';

CREATE TABLE report_approval_events (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations (id),
    evaluation_id UUID NOT NULL REFERENCES evaluations (id) ON DELETE CASCADE,
    from_stage VARCHAR(30) NOT NULL,
    to_stage VARCHAR(30) NOT NULL,
    decision VARCHAR(20) NOT NULL,
    comment VARCHAR(2000),
    actor_user_id UUID,
    actor_name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_report_approval_events_evaluation ON report_approval_events (evaluation_id, created_at);

-- The issued Final Report is frozen at approval so later edits (findings,
-- benchmarks, skills ratings) can't change what the Board approved.
CREATE TABLE final_reports (
    evaluation_id UUID PRIMARY KEY REFERENCES evaluations (id) ON DELETE CASCADE,
    organization_id UUID NOT NULL REFERENCES organizations (id),
    file_name VARCHAR(255) NOT NULL,
    html TEXT NOT NULL,
    issued_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);
