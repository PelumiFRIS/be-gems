-- Governance framework registry, seeded (not admin-editable in MVP — see plan).
CREATE TABLE frameworks (
    id UUID PRIMARY KEY,
    code VARCHAR(40) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    version VARCHAR(20) NOT NULL,
    effective_date DATE,
    is_active BOOLEAN NOT NULL DEFAULT true
);

-- The 18 evaluation dimensions (memo section 12). default_weight_pct rows sum to
-- exactly 100.00 and drive the plain "Overall Board Score"; bgei_category groups
-- dimensions for the separate Board Governance Effectiveness Index (memo section 40).
CREATE TABLE dimensions (
    id UUID PRIMARY KEY,
    framework_id UUID NOT NULL REFERENCES frameworks (id),
    code VARCHAR(60) NOT NULL,
    name VARCHAR(120) NOT NULL,
    display_order INT NOT NULL,
    default_weight_pct NUMERIC(5,2) NOT NULL,
    bgei_category VARCHAR(60)
);

CREATE INDEX idx_dimensions_framework_id ON dimensions (framework_id);

-- Question bank, seeded from the memo's two example questionnaires (Board
-- self-assessment and Director Peer-to-Peer) as real starter data.
CREATE TABLE questions (
    id UUID PRIMARY KEY,
    framework_id UUID NOT NULL REFERENCES frameworks (id),
    dimension_id UUID NOT NULL REFERENCES dimensions (id),
    evaluation_type VARCHAR(20) NOT NULL,
    text TEXT NOT NULL,
    response_type VARCHAR(20) NOT NULL,
    display_order INT NOT NULL,
    is_mandatory BOOLEAN NOT NULL DEFAULT true
);

CREATE INDEX idx_questions_framework_id_evaluation_type ON questions (framework_id, evaluation_type);

-- Governance maturity bands (memo section 18).
CREATE TABLE maturity_levels (
    level INT PRIMARY KEY,
    label VARCHAR(20) NOT NULL,
    min_score NUMERIC(3,2) NOT NULL,
    max_score NUMERIC(3,2) NOT NULL,
    description VARCHAR(255) NOT NULL
);

-- Board Governance Effectiveness Index classification bands (memo section 40).
CREATE TABLE bgei_bands (
    min_pct NUMERIC(5,2) PRIMARY KEY,
    max_pct NUMERIC(5,2) NOT NULL,
    label VARCHAR(60) NOT NULL
);

INSERT INTO frameworks (id, code, name, version, is_active) VALUES
('10000000-0000-0000-0000-000000000001', 'NCCG_2018', 'Nigerian Code of Corporate Governance', '2018', true);

INSERT INTO dimensions (id, framework_id, code, name, display_order, default_weight_pct, bgei_category) VALUES
('20000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001', 'BOARD_COMPOSITION', 'Board Composition', 1, 5.00, 'Board Composition'),
('20000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000001', 'BOARD_STRUCTURE', 'Board Structure', 2, 5.00, 'Board Composition'),
('20000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000001', 'BOARD_LEADERSHIP', 'Board Leadership', 3, 10.00, 'Board Leadership'),
('20000000-0000-0000-0000-000000000004', '10000000-0000-0000-0000-000000000001', 'STRATEGY', 'Strategy', 4, 15.00, 'Strategy'),
('20000000-0000-0000-0000-000000000005', '10000000-0000-0000-0000-000000000001', 'RISK_MANAGEMENT', 'Risk Management', 5, 15.00, 'Risk'),
('20000000-0000-0000-0000-000000000006', '10000000-0000-0000-0000-000000000001', 'FINANCIAL_OVERSIGHT', 'Financial Oversight', 6, 5.00, 'Financial Oversight'),
('20000000-0000-0000-0000-000000000007', '10000000-0000-0000-0000-000000000001', 'AUDIT_INTERNAL_CONTROLS', 'Audit & Internal Controls', 7, 5.00, 'Financial Oversight'),
('20000000-0000-0000-0000-000000000008', '10000000-0000-0000-0000-000000000001', 'COMPLIANCE', 'Compliance', 8, 3.34, 'Compliance'),
('20000000-0000-0000-0000-000000000009', '10000000-0000-0000-0000-000000000001', 'BOARD_MEETINGS', 'Board Meetings', 9, 2.50, 'Board Dynamics'),
('20000000-0000-0000-0000-000000000010', '10000000-0000-0000-0000-000000000001', 'BOARD_INFORMATION', 'Board Information', 10, 2.50, 'Board Dynamics'),
('20000000-0000-0000-0000-000000000011', '10000000-0000-0000-0000-000000000001', 'BOARD_DYNAMICS', 'Board Dynamics', 11, 2.50, 'Board Dynamics'),
('20000000-0000-0000-0000-000000000012', '10000000-0000-0000-0000-000000000001', 'DIRECTOR_PERFORMANCE', 'Director Performance', 12, 2.50, 'Board Dynamics'),
('20000000-0000-0000-0000-000000000013', '10000000-0000-0000-0000-000000000001', 'COMMITTEES', 'Committees', 13, 10.00, 'Committees'),
('20000000-0000-0000-0000-000000000014', '10000000-0000-0000-0000-000000000001', 'ETHICS_CULTURE', 'Ethics & Culture', 14, 3.33, 'Compliance'),
('20000000-0000-0000-0000-000000000015', '10000000-0000-0000-0000-000000000001', 'SUCCESSION_PLANNING', 'Succession Planning', 15, 5.00, 'Succession'),
('20000000-0000-0000-0000-000000000016', '10000000-0000-0000-0000-000000000001', 'STAKEHOLDER_MANAGEMENT', 'Stakeholder Management', 16, 3.33, 'Compliance'),
('20000000-0000-0000-0000-000000000017', '10000000-0000-0000-0000-000000000001', 'TECHNOLOGY_CYBER_OVERSIGHT', 'Technology & Cyber Oversight', 17, 2.50, 'ESG/Technology'),
('20000000-0000-0000-0000-000000000018', '10000000-0000-0000-0000-000000000001', 'ESG_SUSTAINABILITY', 'ESG/Sustainability', 18, 2.50, 'ESG/Technology');

INSERT INTO maturity_levels (level, label, min_score, max_score, description) VALUES
(0, 'Absent', 0.00, 0.99, 'Governance practice does not meaningfully exist'),
(1, 'Initial', 1.00, 1.99, 'Ad hoc/reactive — processes are largely informal or inconsistent'),
(2, 'Developing', 2.00, 2.99, 'Basic structures exist but implementation is inconsistent and requires strengthening'),
(3, 'Defined', 3.00, 3.69, 'Formal governance architecture and processes established — documented and consistently applied'),
(4, 'Managed', 3.70, 4.49, 'Governance performance is actively implemented, consistently monitored and measured'),
(5, 'Optimised', 4.50, 5.00, 'Governance is proactive, integrated, continuously improved and demonstrates best practice');

INSERT INTO bgei_bands (min_pct, max_pct, label) VALUES
(90.00, 100.00, 'Exceptional'),
(80.00, 89.99, 'Highly Effective'),
(70.00, 79.99, 'Effective'),
(60.00, 69.99, 'Developing'),
(50.00, 59.99, 'Needs Significant Improvement'),
(0.00, 49.99, 'Critical');

-- ===================== Board questionnaire (evaluation_type = BOARD) =====================
-- Section A: Board Composition & Structure
INSERT INTO questions (id, framework_id, dimension_id, evaluation_type, text, response_type, display_order, is_mandatory) VALUES
('30000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000001', 'BOARD', 'The composition of the Board is appropriate.', 'RATING_1_5', 1, true),
('30000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000001', 'BOARD', 'The Board possesses the required mix of skills for the business and industry.', 'RATING_1_5', 2, true),
('30000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000002', 'BOARD', 'There is an appropriate balance of executive and non-executive directors.', 'RATING_1_5', 1, true),
('30000000-0000-0000-0000-000000000004', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000002', 'BOARD', 'Independent directors are sufficiently independent.', 'RATING_1_5', 2, true);

-- Section B: Board Leadership
INSERT INTO questions (id, framework_id, dimension_id, evaluation_type, text, response_type, display_order, is_mandatory) VALUES
('30000000-0000-0000-0000-000000000005', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000003', 'BOARD', 'The Chairman is knowledgeable, experienced and effective.', 'RATING_1_5', 1, true),
('30000000-0000-0000-0000-000000000006', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000003', 'BOARD', 'There is an appropriate relationship between the Chairman and CEO.', 'RATING_1_5', 2, true),
('30000000-0000-0000-0000-000000000007', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000003', 'BOARD', 'The Chairman ensures adequate participation and constructive challenge by all directors.', 'RATING_1_5', 3, true);

-- Section C: Strategy
INSERT INTO questions (id, framework_id, dimension_id, evaluation_type, text, response_type, display_order, is_mandatory) VALUES
('30000000-0000-0000-0000-000000000008', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000004', 'BOARD', 'The Board sets the organisation''s overall strategy and provides effective strategic oversight.', 'RATING_1_5', 1, true),
('30000000-0000-0000-0000-000000000009', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000004', 'BOARD', 'The Board monitors execution of approved strategy.', 'RATING_1_5', 2, true);

-- Section D: Risk Management
INSERT INTO questions (id, framework_id, dimension_id, evaluation_type, text, response_type, display_order, is_mandatory) VALUES
('30000000-0000-0000-0000-000000000010', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000005', 'BOARD', 'The Board understands the organisation''s principal risks.', 'RATING_1_5', 1, true),
('30000000-0000-0000-0000-000000000011', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000005', 'BOARD', 'The Board receives adequate risk information.', 'RATING_1_5', 2, true),
('30000000-0000-0000-0000-000000000012', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000005', 'BOARD', 'The organisation''s risk appetite is clearly and appropriately defined and monitored.', 'RATING_1_5', 3, true);

-- Section E: Financial Oversight
INSERT INTO questions (id, framework_id, dimension_id, evaluation_type, text, response_type, display_order, is_mandatory) VALUES
('30000000-0000-0000-0000-000000000013', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000006', 'BOARD', 'The Board understands key financial indicators.', 'RATING_1_5', 1, true),
('30000000-0000-0000-0000-000000000014', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000006', 'BOARD', 'The Board adequately scrutinises financial performance and financial statements are appropriately reviewed.', 'RATING_1_5', 2, true);

-- Section F: Audit & Internal Controls
INSERT INTO questions (id, framework_id, dimension_id, evaluation_type, text, response_type, display_order, is_mandatory) VALUES
('30000000-0000-0000-0000-000000000015', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000007', 'BOARD', 'The Audit Committee is effective.', 'RATING_1_5', 1, true),
('30000000-0000-0000-0000-000000000016', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000007', 'BOARD', 'Internal audit is sufficiently independent.', 'RATING_1_5', 2, true),
('30000000-0000-0000-0000-000000000017', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000007', 'BOARD', 'Control weaknesses are appropriately escalated.', 'RATING_1_5', 3, true);

-- Section G: Compliance & Regulatory Oversight
INSERT INTO questions (id, framework_id, dimension_id, evaluation_type, text, response_type, display_order, is_mandatory) VALUES
('30000000-0000-0000-0000-000000000018', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000008', 'BOARD', 'The Board receives timely regulatory information.', 'RATING_1_5', 1, true),
('30000000-0000-0000-0000-000000000019', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000008', 'BOARD', 'Regulatory breaches are appropriately reported without delay.', 'RATING_1_5', 2, true),
('30000000-0000-0000-0000-000000000020', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000008', 'BOARD', 'The Board monitors compliance with applicable laws and regulations.', 'RATING_1_5', 3, true);

-- Section H: Board Meetings
INSERT INTO questions (id, framework_id, dimension_id, evaluation_type, text, response_type, display_order, is_mandatory) VALUES
('30000000-0000-0000-0000-000000000021', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000009', 'BOARD', 'Meeting agendas are appropriately structured.', 'RATING_1_5', 1, true),
('30000000-0000-0000-0000-000000000022', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000009', 'BOARD', 'Board papers/packs are received sufficiently early and no later than 48 hours before meetings.', 'RATING_1_5', 2, true),
('30000000-0000-0000-0000-000000000023', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000009', 'BOARD', 'Sufficient time is allocated to major issues.', 'RATING_1_5', 3, true),
('30000000-0000-0000-0000-000000000024', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000009', 'BOARD', 'Discussions are substantive rather than procedural.', 'RATING_1_5', 4, true);

-- Section I: Board Information
INSERT INTO questions (id, framework_id, dimension_id, evaluation_type, text, response_type, display_order, is_mandatory) VALUES
('30000000-0000-0000-0000-000000000025', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000010', 'BOARD', 'Information dissemination is accurate and timely.', 'RATING_1_5', 1, true),
('30000000-0000-0000-0000-000000000026', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000010', 'BOARD', 'Information disseminated is sufficiently detailed.', 'RATING_1_5', 2, true),
('30000000-0000-0000-0000-000000000027', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000010', 'BOARD', 'The Board receives appropriate management information and reports.', 'RATING_1_5', 3, true);

-- Section J: Board Dynamics
INSERT INTO questions (id, framework_id, dimension_id, evaluation_type, text, response_type, display_order, is_mandatory) VALUES
('30000000-0000-0000-0000-000000000028', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000011', 'BOARD', 'Members constructively challenge themselves.', 'RATING_1_5', 1, true),
('30000000-0000-0000-0000-000000000029', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000011', 'BOARD', 'Dissenting views are encouraged without backlash.', 'RATING_1_5', 2, true),
('30000000-0000-0000-0000-000000000030', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000011', 'BOARD', 'Directors feel comfortable expressing independent opinions.', 'RATING_1_5', 3, true),
('30000000-0000-0000-0000-000000000031', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000011', 'BOARD', 'There is mutual respect among members.', 'RATING_1_5', 4, true);

-- Section K: Committees
INSERT INTO questions (id, framework_id, dimension_id, evaluation_type, text, response_type, display_order, is_mandatory) VALUES
('30000000-0000-0000-0000-000000000032', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000013', 'BOARD', 'Committees are appropriately constituted.', 'RATING_1_5', 1, true),
('30000000-0000-0000-0000-000000000033', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000013', 'BOARD', 'Committee mandates are clear and properly spelt out.', 'RATING_1_5', 2, true),
('30000000-0000-0000-0000-000000000034', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000013', 'BOARD', 'Committees report effectively to the board.', 'RATING_1_5', 3, true),
('30000000-0000-0000-0000-000000000035', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000013', 'BOARD', 'Committee recommendations are adequately considered by the board.', 'RATING_1_5', 4, true);

-- Section L: Director Performance
INSERT INTO questions (id, framework_id, dimension_id, evaluation_type, text, response_type, display_order, is_mandatory) VALUES
('30000000-0000-0000-0000-000000000036', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000012', 'BOARD', 'Directors attend meetings regularly.', 'RATING_1_5', 1, true),
('30000000-0000-0000-0000-000000000037', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000012', 'BOARD', 'Directors prepare adequately for meetings.', 'RATING_1_5', 2, true),
('30000000-0000-0000-0000-000000000038', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000012', 'BOARD', 'Directors understand their fiduciary responsibilities.', 'RATING_1_5', 3, true),
('30000000-0000-0000-0000-000000000039', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000012', 'BOARD', 'Directors participate and meaningfully contribute to items on the meeting agenda.', 'RATING_1_5', 4, true);

-- Section M + Q: Ethics & Culture (incl. Whistleblowing Policy)
INSERT INTO questions (id, framework_id, dimension_id, evaluation_type, text, response_type, display_order, is_mandatory) VALUES
('30000000-0000-0000-0000-000000000040', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000014', 'BOARD', 'The Board sets the appropriate tone at the top.', 'RATING_1_5', 1, true),
('30000000-0000-0000-0000-000000000041', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000014', 'BOARD', 'The Board promotes ethical behaviour.', 'RATING_1_5', 2, true),
('30000000-0000-0000-0000-000000000042', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000014', 'BOARD', 'Conflicts of interest are appropriately managed.', 'RATING_1_5', 3, true),
('30000000-0000-0000-0000-000000000043', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000014', 'BOARD', 'The company has a whistleblowing policy that is enforceable and approved by the board.', 'RATING_1_5', 4, true),
('30000000-0000-0000-0000-000000000044', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000014', 'BOARD', 'Reporting channels are easy to access for all relevant groups, including remote workers, non-employees and contractors, where applicable.', 'RATING_1_5', 5, true),
('30000000-0000-0000-0000-000000000045', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000014', 'BOARD', 'The function responsible for handling reports is sufficiently independent from the business areas being reported about, and there are tested/validated safeguards to prevent retaliation.', 'RATING_1_5', 6, true);

-- Section N: Succession Planning
INSERT INTO questions (id, framework_id, dimension_id, evaluation_type, text, response_type, display_order, is_mandatory) VALUES
('30000000-0000-0000-0000-000000000046', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000015', 'BOARD', 'Board succession is adequately planned and mapped out.', 'RATING_1_5', 1, true),
('30000000-0000-0000-0000-000000000047', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000015', 'BOARD', 'Management succession is adequately addressed and provided for.', 'RATING_1_5', 2, true),
('30000000-0000-0000-0000-000000000048', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000015', 'BOARD', 'The organisation has an appropriate pipeline of leadership talent.', 'RATING_1_5', 3, true);

-- Section O: ESG/Sustainability
INSERT INTO questions (id, framework_id, dimension_id, evaluation_type, text, response_type, display_order, is_mandatory) VALUES
('30000000-0000-0000-0000-000000000049', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000018', 'BOARD', 'The Board understands material ESG risks.', 'RATING_1_5', 1, true),
('30000000-0000-0000-0000-000000000050', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000018', 'BOARD', 'ESG is integrated into strategy and risk management.', 'RATING_1_5', 2, true),
('30000000-0000-0000-0000-000000000051', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000018', 'BOARD', 'The Board monitors sustainability objectives.', 'RATING_1_5', 3, true);

-- Section P: Stakeholder Management
INSERT INTO questions (id, framework_id, dimension_id, evaluation_type, text, response_type, display_order, is_mandatory) VALUES
('30000000-0000-0000-0000-000000000052', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000016', 'BOARD', 'The board ensures that the company maintains an up-to-date map of key stakeholder groups (customers, employees, regulators, partners, communities).', 'RATING_1_5', 1, true),
('30000000-0000-0000-0000-000000000053', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000016', 'BOARD', 'Stakeholders'' needs are prioritised and translated into measurable company objectives.', 'RATING_1_5', 2, true),
('30000000-0000-0000-0000-000000000054', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000016', 'BOARD', 'The board has mechanisms to ensure it hears from stakeholders directly (e.g., customer/user advisory, employee forums, regulator engagement feedback).', 'RATING_1_5', 3, true);

-- Section R: Technology & Cyber Oversight
INSERT INTO questions (id, framework_id, dimension_id, evaluation_type, text, response_type, display_order, is_mandatory) VALUES
('30000000-0000-0000-0000-000000000055', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000017', 'BOARD', 'The board ensures technology strategy is aligned to business strategy and measurable value delivery.', 'RATING_1_5', 1, true),
('30000000-0000-0000-0000-000000000056', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000017', 'BOARD', 'The board reviews plans for failures (site outage, cloud region failure, key service degradation).', 'RATING_1_5', 2, true),
('30000000-0000-0000-0000-000000000057', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000017', 'BOARD', 'The board understands cyber risk in business terms (impact to revenue, operations, safety, compliance, trust).', 'RATING_1_5', 3, true),
('30000000-0000-0000-0000-000000000058', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000017', 'BOARD', 'Cyber risk is clearly owned (roles, accountability, escalation paths), and the board tracks it.', 'RATING_1_5', 4, true);

-- ================= Director Peer-to-Peer questionnaire (evaluation_type = DIRECTOR_PEER) =================
-- Section A: Preparation, contribution, and challenge (items 1-10) -> Director Performance
INSERT INTO questions (id, framework_id, dimension_id, evaluation_type, text, response_type, display_order, is_mandatory) VALUES
('30000000-0000-0000-0000-000000000059', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000012', 'DIRECTOR_PEER', 'Prepares thoroughly and comes to meetings ready.', 'RATING_1_5', 1, true),
('30000000-0000-0000-0000-000000000060', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000012', 'DIRECTOR_PEER', 'Reviews board papers in advance; focuses on decision-relevant issues.', 'RATING_1_5', 2, true),
('30000000-0000-0000-0000-000000000061', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000012', 'DIRECTOR_PEER', 'Asks clear, incisive questions that improve decisions.', 'RATING_1_5', 3, true),
('30000000-0000-0000-0000-000000000062', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000012', 'DIRECTOR_PEER', 'Challenges assumptions where evidence is insufficient.', 'RATING_1_5', 4, true),
('30000000-0000-0000-0000-000000000063', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000012', 'DIRECTOR_PEER', 'Contributes constructively without dominating or dismissing.', 'RATING_1_5', 5, true),
('30000000-0000-0000-0000-000000000064', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000012', 'DIRECTOR_PEER', 'Adds value using relevant expertise and experience.', 'RATING_1_5', 6, true),
('30000000-0000-0000-0000-000000000065', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000012', 'DIRECTOR_PEER', 'Balances strategic ambition with execution feasibility.', 'RATING_1_5', 7, true),
('30000000-0000-0000-0000-000000000066', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000012', 'DIRECTOR_PEER', 'Keeps discussions outcome/risk-focused rather than process-focused.', 'RATING_1_5', 8, true),
('30000000-0000-0000-0000-000000000067', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000012', 'DIRECTOR_PEER', 'Raises emerging material concerns early.', 'RATING_1_5', 9, true),
('30000000-0000-0000-0000-000000000068', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000012', 'DIRECTOR_PEER', 'Demonstrates sound judgment when information is incomplete.', 'RATING_1_5', 10, true);

-- Section B: Risk oversight, assurance, and compliance (items 11-16) -> Risk Management (11-13), Compliance (14-16)
INSERT INTO questions (id, framework_id, dimension_id, evaluation_type, text, response_type, display_order, is_mandatory) VALUES
('30000000-0000-0000-0000-000000000069', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000005', 'DIRECTOR_PEER', 'Holds management accountable for risk ownership and outcomes.', 'RATING_1_5', 4, true),
('30000000-0000-0000-0000-000000000070', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000005', 'DIRECTOR_PEER', 'Challenges whether risk appetite/controls/assurance are fit for purpose.', 'RATING_1_5', 5, true),
('30000000-0000-0000-0000-000000000071', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000005', 'DIRECTOR_PEER', 'Helps ensure independent assurance is credible and acted on.', 'RATING_1_5', 6, true),
('30000000-0000-0000-0000-000000000072', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000008', 'DIRECTOR_PEER', 'Challenges whether compliance, ethics, and conduct risks are effectively managed.', 'RATING_1_5', 1, true),
('30000000-0000-0000-0000-000000000073', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000008', 'DIRECTOR_PEER', 'Escalates issues appropriately and supports timely remediation.', 'RATING_1_5', 2, true),
('30000000-0000-0000-0000-000000000074', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000008', 'DIRECTOR_PEER', 'Ensures lessons learned from audits/incidents/near-misses drive fixes.', 'RATING_1_5', 3, true);

-- Section C: Technology/cyber, data, and operational oversight (items 17-18) -> Technology & Cyber Oversight
INSERT INTO questions (id, framework_id, dimension_id, evaluation_type, text, response_type, display_order, is_mandatory) VALUES
('30000000-0000-0000-0000-000000000075', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000017', 'DIRECTOR_PEER', 'Demonstrates sufficient understanding to challenge technology/operational performance risks.', 'RATING_1_5', 5, true),
('30000000-0000-0000-0000-000000000076', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000017', 'DIRECTOR_PEER', 'Ensures oversight includes evidence (metrics/incidents/control testing) and clear accountability.', 'RATING_1_5', 6, true);

-- Section D: Stakeholder, culture, and integrity (items 19-22) -> Stakeholder Management (19), Ethics & Culture (20-22)
INSERT INTO questions (id, framework_id, dimension_id, evaluation_type, text, response_type, display_order, is_mandatory) VALUES
('30000000-0000-0000-0000-000000000077', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000016', 'DIRECTOR_PEER', 'Considers stakeholder impacts in board discussions and decisions.', 'RATING_1_5', 4, true),
('30000000-0000-0000-0000-000000000078', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000014', 'DIRECTOR_PEER', 'Reinforces ethical culture and "tone from the top."', 'RATING_1_5', 7, true),
('30000000-0000-0000-0000-000000000079', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000014', 'DIRECTOR_PEER', 'Handles sensitive matters (e.g., investigations, whistleblowing) with discretion and rigor.', 'RATING_1_5', 8, true),
('30000000-0000-0000-0000-000000000080', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000014', 'DIRECTOR_PEER', 'Supports strong speak-up mechanisms and non-retaliation principles.', 'RATING_1_5', 9, true);

-- Section E: Board dynamics, professionalism, and collaboration (items 23-25) -> Board Dynamics
INSERT INTO questions (id, framework_id, dimension_id, evaluation_type, text, response_type, display_order, is_mandatory) VALUES
('30000000-0000-0000-0000-000000000081', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000011', 'DIRECTOR_PEER', 'Collaborates effectively with fellow directors and committee members.', 'RATING_1_5', 5, true),
('30000000-0000-0000-0000-000000000082', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000011', 'DIRECTOR_PEER', 'Communicates respectfully; sustains constructive debate under disagreement.', 'RATING_1_5', 6, true),
('30000000-0000-0000-0000-000000000083', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000011', 'DIRECTOR_PEER', 'Overall: the board benefits from this director''s contribution (challenge, prep, discretion, follow-through).', 'RATING_1_5', 7, true);

-- Section F: Recommendations (narrative, not scored)
INSERT INTO questions (id, framework_id, dimension_id, evaluation_type, text, response_type, display_order, is_mandatory) VALUES
('30000000-0000-0000-0000-000000000084', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000012', 'DIRECTOR_PEER', 'What should this director continue doing?', 'NARRATIVE', 11, false),
('30000000-0000-0000-0000-000000000085', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000012', 'DIRECTOR_PEER', 'What is one improvement to make next?', 'NARRATIVE', 12, false),
('30000000-0000-0000-0000-000000000086', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000012', 'DIRECTOR_PEER', 'Any board-level gaps to address (training/briefings/committee support)?', 'NARRATIVE', 13, false);
