CREATE TABLE finding_attachments (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations (id),
    finding_id UUID NOT NULL REFERENCES findings (id),
    file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    file_data BYTEA NOT NULL,
    uploaded_by UUID NOT NULL REFERENCES users (id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);
CREATE INDEX idx_finding_attachments_finding_id ON finding_attachments (finding_id);
