package com.fris.begems.skill;

/**
 * Memo §8 gap categories. A director counts as proficient at 4 (Advanced) or 5
 * (Expert) and as competent from 3. How many of them a competency needs depends
 * on its Board Requirement — see {@link SkillsAnalysis}.
 */
public enum SkillCoverage {
    COVERED,
    UNDERREPRESENTED,
    SINGLE_PERSON_DEPENDENCY,
    CRITICAL_GAP,
    NOT_ASSESSED
}
