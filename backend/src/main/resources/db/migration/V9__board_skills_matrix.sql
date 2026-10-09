-- Memo §8: a configurable competency framework per board, each director rated
-- 1 (Basic) to 5 (Expert), and a Board Requirement (LOW/MEDIUM/HIGH) per competency.
CREATE TABLE board_skills (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations (id),
    board_id UUID NOT NULL REFERENCES boards (id),
    name VARCHAR(120) NOT NULL,
    required_level VARCHAR(10) NOT NULL,
    future_focus BOOLEAN NOT NULL DEFAULT FALSE,
    display_order INTEGER NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_board_skills_board_name ON board_skills (board_id, lower(name));

CREATE TABLE director_skill_ratings (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations (id),
    director_id UUID NOT NULL REFERENCES directors (id) ON DELETE CASCADE,
    skill_id UUID NOT NULL REFERENCES board_skills (id) ON DELETE CASCADE,
    rating INTEGER NOT NULL CHECK (rating BETWEEN 1 AND 5),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_director_skill_ratings_director_skill ON director_skill_ratings (director_id, skill_id);
CREATE INDEX idx_director_skill_ratings_skill_id ON director_skill_ratings (skill_id);

-- Existing boards get the memo's default framework; new boards get it from
-- DefaultSkills when they're created. Keep the two lists in step.
INSERT INTO board_skills (id, organization_id, board_id, name, required_level, future_focus, display_order)
SELECT gen_random_uuid(), b.organization_id, b.id, d.name, d.required_level, d.future_focus, d.display_order
FROM boards b
CROSS JOIN (VALUES
    ('Strategy', 'HIGH', FALSE, 1),
    ('Finance', 'HIGH', FALSE, 2),
    ('Accounting', 'MEDIUM', FALSE, 3),
    ('Audit', 'HIGH', FALSE, 4),
    ('Risk', 'HIGH', FALSE, 5),
    ('Banking', 'MEDIUM', FALSE, 6),
    ('Legal', 'MEDIUM', FALSE, 7),
    ('Regulatory', 'MEDIUM', FALSE, 8),
    ('Technology', 'HIGH', TRUE, 9),
    ('Cybersecurity', 'MEDIUM', TRUE, 10),
    ('Digital transformation', 'MEDIUM', TRUE, 11),
    ('Human resources', 'LOW', FALSE, 12),
    ('ESG', 'MEDIUM', TRUE, 13),
    ('International business', 'LOW', FALSE, 14),
    ('Industry knowledge', 'HIGH', FALSE, 15),
    ('Capital markets', 'MEDIUM', FALSE, 16),
    ('Marketing', 'LOW', FALSE, 17),
    ('Corporate governance', 'HIGH', FALSE, 18)
) AS d (name, required_level, future_focus, display_order);
