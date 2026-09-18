ALTER TABLE directors ADD COLUMN user_id UUID REFERENCES users (id);
CREATE UNIQUE INDEX idx_directors_user_id ON directors (user_id) WHERE user_id IS NOT NULL;

CREATE TABLE evaluations (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations (id),
    board_id UUID NOT NULL REFERENCES boards (id),
    framework_id UUID NOT NULL REFERENCES frameworks (id),
    evaluation_type VARCHAR(20) NOT NULL,
    subject_director_id UUID REFERENCES directors (id),
    year INT NOT NULL,
    status VARCHAR(20) NOT NULL,
    start_date DATE,
    close_date DATE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_evaluations_board_id ON evaluations (board_id);

CREATE TABLE evaluation_respondents (
    id UUID PRIMARY KEY,
    evaluation_id UUID NOT NULL REFERENCES evaluations (id),
    director_id UUID NOT NULL REFERENCES directors (id),
    confidentiality_mode VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    submitted_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    UNIQUE (evaluation_id, director_id)
);

CREATE INDEX idx_evaluation_respondents_evaluation_id ON evaluation_respondents (evaluation_id);
CREATE INDEX idx_evaluation_respondents_director_id ON evaluation_respondents (director_id);

CREATE TABLE responses (
    id UUID PRIMARY KEY,
    evaluation_respondent_id UUID NOT NULL REFERENCES evaluation_respondents (id),
    question_id UUID NOT NULL REFERENCES questions (id),
    rating_value INT,
    text_value TEXT,
    numeric_value NUMERIC(10,2),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    UNIQUE (evaluation_respondent_id, question_id)
);

CREATE INDEX idx_responses_evaluation_respondent_id ON responses (evaluation_respondent_id);

CREATE TABLE evaluation_scores (
    id UUID PRIMARY KEY,
    evaluation_id UUID NOT NULL REFERENCES evaluations (id),
    scope_type VARCHAR(30) NOT NULL,
    dimension_id UUID REFERENCES dimensions (id),
    bgei_category VARCHAR(60),
    raw_score NUMERIC(4,2),
    weighted_score NUMERIC(6,2),
    maturity_level INT,
    bgei_band_label VARCHAR(60),
    computed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_evaluation_scores_evaluation_id ON evaluation_scores (evaluation_id);
