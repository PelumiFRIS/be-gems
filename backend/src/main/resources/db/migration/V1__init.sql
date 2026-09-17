CREATE TABLE organizations (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE TABLE users (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations (id),
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    role VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_users_organization_id ON users (organization_id);

CREATE TABLE boards (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations (id),
    name VARCHAR(255) NOT NULL,
    effective_date DATE,
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_boards_organization_id ON boards (organization_id);

CREATE TABLE directors (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations (id),
    board_id UUID NOT NULL REFERENCES boards (id),
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    classification VARCHAR(40) NOT NULL,
    appointment_date DATE,
    term_expiration_date DATE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_directors_board_id ON directors (board_id);

CREATE TABLE committees (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations (id),
    board_id UUID NOT NULL REFERENCES boards (id),
    name VARCHAR(255) NOT NULL,
    meeting_frequency VARCHAR(100),
    mandate TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_committees_board_id ON committees (board_id);

CREATE TABLE committee_members (
    id UUID PRIMARY KEY,
    committee_id UUID NOT NULL REFERENCES committees (id),
    director_id UUID NOT NULL REFERENCES directors (id),
    role VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_committee_members_committee_id ON committee_members (committee_id);

CREATE TABLE audit_logs (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations (id),
    actor_user_id UUID,
    actor_name VARCHAR(255) NOT NULL,
    action VARCHAR(60) NOT NULL,
    entity_type VARCHAR(40) NOT NULL,
    entity_id UUID,
    summary VARCHAR(500) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_audit_logs_organization_id_created_at ON audit_logs (organization_id, created_at DESC);
