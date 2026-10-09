-- Memo §19: each organisation can set its own benchmark per governance dimension
-- (score out of 5) and for the BGEI (percentage, stored with dimension_id NULL).
-- No row means the BE-GEMS default applies.
CREATE TABLE governance_benchmarks (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations (id),
    dimension_id UUID REFERENCES dimensions (id),
    target NUMERIC(5,2) NOT NULL,
    source VARCHAR(200),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_governance_benchmarks_dimension ON governance_benchmarks (organization_id, dimension_id)
    WHERE dimension_id IS NOT NULL;
CREATE UNIQUE INDEX uq_governance_benchmarks_bgei ON governance_benchmarks (organization_id)
    WHERE dimension_id IS NULL;
