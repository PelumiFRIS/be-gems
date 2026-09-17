package com.fris.begems.user;

/**
 * Trimmed to 5 roles for MVP; each SRS role maps onto exactly one of these so a
 * later split (e.g. CHAIRMAN out of DIRECTOR) is additive, not a rewrite:
 *   SUPER_ADMIN        - Super Administrator
 *   ORG_ADMIN           - Organisation Administrator, System Administration
 *   COMPANY_SECRETARY   - Company Secretary, Board Administrator, Report Approver
 *   EVALUATOR           - External Evaluator, Auditor/Reviewer
 *   DIRECTOR            - Chairman, Director, Committee Chair/Member, CEO/MD, Management
 * Chairman/committee-chair/CEO distinctions live as data on Director.classification,
 * not as separate roles.
 */
public enum Role {
    SUPER_ADMIN,
    ORG_ADMIN,
    COMPANY_SECRETARY,
    EVALUATOR,
    DIRECTOR
}
