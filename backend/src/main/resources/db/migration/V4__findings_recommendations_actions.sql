CREATE TABLE findings (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations (id),
    evaluation_id UUID NOT NULL REFERENCES evaluations (id),
    dimension_id UUID REFERENCES dimensions (id),
    description TEXT NOT NULL,
    severity VARCHAR(20) NOT NULL,
    evidence TEXT,
    regulatory_reference TEXT,
    root_cause TEXT,
    risk_implication TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_findings_evaluation_id ON findings (evaluation_id);

CREATE TABLE recommendations (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations (id),
    finding_id UUID NOT NULL REFERENCES findings (id),
    recommended_action TEXT NOT NULL,
    responsible_person VARCHAR(255),
    committee_responsible VARCHAR(255),
    target_date DATE,
    priority VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_recommendations_finding_id ON recommendations (finding_id);

CREATE TABLE corrective_actions (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations (id),
    finding_id UUID NOT NULL REFERENCES findings (id),
    description TEXT NOT NULL,
    owner VARCHAR(255),
    approver VARCHAR(255),
    due_date DATE,
    status VARCHAR(20) NOT NULL,
    evidence TEXT,
    closure_date DATE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_corrective_actions_finding_id ON corrective_actions (finding_id);
CREATE INDEX idx_corrective_actions_organization_id ON corrective_actions (organization_id);
