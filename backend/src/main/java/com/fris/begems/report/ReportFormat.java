package com.fris.begems.report;

import com.fris.begems.director.DirectorClassification;
import com.fris.begems.skill.SkillCoverage;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

final class ReportFormat {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.UK);

    private ReportFormat() {
    }

    static String date(LocalDate date) {
        return date == null ? null : DATE.format(date);
    }

    static String period(LocalDate start, LocalDate close) {
        if (start == null && close == null) {
            return "Not recorded";
        }
        return (start == null ? "—" : date(start)) + " to " + (close == null ? "—" : date(close));
    }

    static String score(BigDecimal value) {
        return value == null ? null : round(value).toPlainString();
    }

    static String outOfFive(BigDecimal value) {
        return value == null ? "—" : score(value) + " / 5.00";
    }

    static String percent(BigDecimal value) {
        return value == null ? null : round(value).toPlainString() + "%";
    }

    static BigDecimal round(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    static String classification(DirectorClassification classification) {
        return switch (classification) {
            case CHAIRMAN -> "Chairman";
            case EXECUTIVE_DIRECTOR -> "Executive Director";
            case NON_EXECUTIVE_DIRECTOR -> "Non-Executive Director";
            case INDEPENDENT_NON_EXECUTIVE_DIRECTOR -> "Independent Non-Executive Director";
            case MD_CEO -> "MD/CEO";
        };
    }

    static String skillRating(int rating) {
        return rating + " – " + switch (rating) {
            case 1 -> "Basic";
            case 2 -> "Developing";
            case 3 -> "Competent";
            case 4 -> "Advanced";
            default -> "Expert";
        };
    }

    static String skillCoverage(SkillCoverage coverage) {
        return switch (coverage) {
            case COVERED -> "Adequately covered";
            case UNDERREPRESENTED -> "Underrepresented";
            case SINGLE_PERSON_DEPENDENCY -> "Single-person dependency";
            case CRITICAL_GAP -> "Critical gap";
            case NOT_ASSESSED -> "Not assessed";
        };
    }

    /** IN_PROGRESS → "In progress". */
    static String humanize(Enum<?> value) {
        if (value == null) {
            return null;
        }
        String words = value.name().replace('_', ' ').toLowerCase(Locale.ROOT);
        return Character.toUpperCase(words.charAt(0)) + words.substring(1);
    }

    /** ["a", "b", "c"] → "a, b and c". */
    static String joinWithAnd(List<String> items) {
        if (items.isEmpty()) {
            return "";
        }
        if (items.size() == 1) {
            return items.get(0);
        }
        return String.join(", ", items.subList(0, items.size() - 1)) + " and " + items.get(items.size() - 1);
    }

    static String plural(long count, String singular, String pluralForm) {
        return count + " " + (count == 1 ? singular : pluralForm);
    }
}
