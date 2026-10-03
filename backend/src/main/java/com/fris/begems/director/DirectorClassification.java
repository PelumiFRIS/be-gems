package com.fris.begems.director;

/**
 * Carries the Chairman/MD/committee-chair distinctions as data rather than
 * separate auth roles — see Role.java for why. Committee-chair status itself is
 * tracked on CommitteeMember, not here.
 */
public enum DirectorClassification {
    CHAIRMAN,
    EXECUTIVE_DIRECTOR,
    NON_EXECUTIVE_DIRECTOR,
    INDEPENDENT_NON_EXECUTIVE_DIRECTOR,
    MD_CEO
}
