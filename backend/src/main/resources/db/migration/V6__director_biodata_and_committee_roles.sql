ALTER TABLE directors
    ADD COLUMN date_of_birth DATE,
    ADD COLUMN address TEXT,
    ADD COLUMN phone VARCHAR(50),
    ADD COLUMN re_election_date DATE,
    ADD COLUMN profession VARCHAR(255),
    ADD COLUMN qualification TEXT,
    ADD COLUMN experience TEXT;

UPDATE directors SET classification = 'MD_CEO' WHERE classification = 'CEO_MD';

CREATE TABLE director_cvs (
    director_id UUID PRIMARY KEY REFERENCES directors (id),
    organization_id UUID NOT NULL REFERENCES organizations (id),
    file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    file_data BYTEA NOT NULL,
    uploaded_by UUID NOT NULL REFERENCES users (id),
    uploaded_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

-- The old "Set chair" control inserted a new row on every pick, so a director could
-- appear in a committee several times and a committee could have several chairs.
-- Keep each director's most recent assignment, and only the most recent chair.
DELETE FROM committee_members older
USING committee_members newer
WHERE older.committee_id = newer.committee_id
  AND older.director_id = newer.director_id
  AND (older.created_at, older.id) < (newer.created_at, newer.id);

UPDATE committee_members older
SET role = 'MEMBER'
FROM committee_members newer
WHERE older.committee_id = newer.committee_id
  AND older.role = 'CHAIR'
  AND newer.role = 'CHAIR'
  AND (older.created_at, older.id) < (newer.created_at, newer.id);

CREATE UNIQUE INDEX uq_committee_members_committee_director ON committee_members (committee_id, director_id);
CREATE UNIQUE INDEX uq_committee_members_one_chair ON committee_members (committee_id) WHERE role = 'CHAIR';
CREATE INDEX idx_committee_members_director_id ON committee_members (director_id);
