package com.fris.begems.audit;

/**
 * Grows one value at a time as each module is built — see the plan's phased
 * backlog. Only the actions built so far are listed here.
 */
public enum AuditAction {
    ORGANIZATION_SIGNUP,
    LOGIN,
    USER_CREATED,
    DIRECTOR_INVITED,
    EVALUATION_CREATED,
    EVALUATION_LAUNCHED,
    EVALUATION_CLOSED,
    EVALUATION_SCORED,
    FINDING_CREATED,
    RECOMMENDATION_CREATED,
    ACTION_CREATED,
    ACTION_CLOSED,
    ATTACHMENT_UPLOADED
}
