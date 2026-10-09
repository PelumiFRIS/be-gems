package com.fris.begems.report;

import static com.fris.begems.report.ReportFormat.date;
import static com.fris.begems.report.ReportFormat.joinWithAnd;
import static com.fris.begems.report.ReportFormat.outOfFive;
import static com.fris.begems.report.ReportFormat.percent;
import static com.fris.begems.report.ReportFormat.plural;
import static com.fris.begems.report.ReportFormat.score;

import com.fris.begems.action.ActionStatus;
import com.fris.begems.action.CorrectiveAction;
import com.fris.begems.benchmark.BenchmarkAnalysis;
import com.fris.begems.benchmark.Benchmarks;
import com.fris.begems.benchmark.dto.BenchmarkComparison;
import com.fris.begems.benchmark.dto.BenchmarkComparisonRow;
import com.fris.begems.committee.Committee;
import com.fris.begems.committee.CommitteeMember;
import com.fris.begems.committee.CommitteeMemberRole;
import com.fris.begems.director.Director;
import com.fris.begems.director.DirectorClassification;
import com.fris.begems.evaluation.ConfidentialityMode;
import com.fris.begems.evaluation.EvaluationRespondent;
import com.fris.begems.evaluation.RespondentStatus;
import com.fris.begems.finding.Finding;
import com.fris.begems.finding.FindingSeverity;
import com.fris.begems.framework.Dimension;
import com.fris.begems.framework.Question;
import com.fris.begems.recommendation.Recommendation;
import com.fris.begems.report.ReportHtmlBuilder.Column;
import com.fris.begems.report.ResponseStatistics.QuestionResult;
import com.fris.begems.scoring.BgeiBand;
import com.fris.begems.scoring.EvaluationScore;
import com.fris.begems.scoring.MaturityLevel;
import com.fris.begems.scoring.ScoreScopeType;
import com.fris.begems.skill.SkillCoverage;
import com.fris.begems.skill.dto.SkillRow;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * The full Board Evaluation Report — the 22 standard parts listed in memo §26
 * (cover page and confidentiality statement first, then 19 numbered sections,
 * then appendices). Sections the MVP has no dedicated data for (committee,
 * Chairman and director evaluation) are drawn from the matching Board
 * questionnaire dimensions and say so, rather than being left out.
 */
@Component
class BoardReportWriter {

    private static final BigDecimal STRENGTH_THRESHOLD = new BigDecimal("3.70");
    private static final int HIGHLIGHT_LIMIT = 5;

    byte[] write(ReportContext ctx) {
        Results results = new Results(ctx);
        String org = ctx.organization().getName();
        ReportHtmlBuilder html = new ReportHtmlBuilder(org + " — Board Evaluation Report " + ctx.evaluation().getYear());

        html.cover("Strictly private and confidential", "Board Evaluation Report", org, List.of(
                new String[] {"Board", ctx.board().getName()},
                new String[] {"Evaluation year", String.valueOf(ctx.evaluation().getYear())},
                new String[] {"Evaluation period",
                        ReportFormat.period(ctx.evaluation().getStartDate(), ctx.evaluation().getCloseDate())},
                new String[] {"Governance framework", frameworkName(ctx)},
                new String[] {"Report generated", date(ctx.generatedOn())}));

        html.heading("Confidentiality Statement")
                .paragraph("This report has been prepared for the Board of Directors of " + org + ". It contains "
                        + "confidential information about the performance of the Board, its committees and its "
                        + "members, and must not be copied, distributed or disclosed, in whole or in part, to any "
                        + "third party without the prior written consent of the Board.")
                .paragraph("Individual responses were collected in confidence and are presented in aggregate only. "
                        + "No rating or comment in this report is attributed to an individual director.");
        html.contents();

        executiveSummary(html, ctx, results);
        background(html, ctx);
        objectives(html);
        scope(html, ctx, results);
        methodology(html, ctx, results);
        framework(html, ctx);
        boardComposition(html, ctx);
        effectivenessResults(html, ctx, results);
        committeeResults(html, ctx, results);
        chairmanEvaluation(html, ctx, results);
        directorEvaluation(html, ctx, results);
        governanceMaturity(html, ctx, results);
        regulatoryBenchmark(html, ctx);
        keyStrengths(html, results);
        keyFindings(html, ctx);
        areasForImprovement(html, results);
        recommendations(html, ctx);
        actionPlan(html, ctx);
        conclusion(html, ctx, results);
        appendices(html, ctx, results);

        html.footer(org + " · Board Evaluation Report " + ctx.evaluation().getYear() + " · Generated "
                + date(ctx.generatedOn()) + " by BE-GEMS · Confidential");
        return html.toBytes();
    }

    private void executiveSummary(ReportHtmlBuilder html, ReportContext ctx, Results results) {
        html.section("Executive Summary");
        long criticalOrHigh = ctx.findings().stream()
                .filter(f -> f.getSeverity() == FindingSeverity.CRITICAL || f.getSeverity() == FindingSeverity.HIGH)
                .count();
        html.figures(List.of(
                new String[] {"Governance Effectiveness Index", percentOrDash(results.bgeiPct()), results.bgeiBand()},
                new String[] {"Overall board score", outOfFive(results.boardScore()), results.boardMaturityLabel()},
                new String[] {"Participation", results.submitted() + " of " + results.invited(), "questionnaires submitted"},
                new String[] {"Findings", String.valueOf(ctx.findings().size()),
                        criticalOrHigh + " critical or high severity"}));

        StringBuilder headline = new StringBuilder("The ").append(ctx.evaluation().getYear())
                .append(" evaluation of the ").append(ctx.board().getName()).append(" of ")
                .append(ctx.organization().getName()).append(" assessed the Board across ")
                .append(plural(results.dimensionScores().size(), "governance dimension", "governance dimensions"))
                .append(" of the ").append(frameworkName(ctx)).append(".");
        if (results.bgeiPct() != null) {
            headline.append(" The Board achieved a Board Governance Effectiveness Index (BGEI) of ")
                    .append(percent(results.bgeiPct())).append(", classified as ").append(results.bgeiBand())
                    .append(",");
        }
        if (results.boardScore() != null) {
            headline.append(" with an overall weighted board score of ").append(score(results.boardScore()))
                    .append(" out of 5.00");
            results.boardMaturity().ifPresent(m -> headline.append(", corresponding to governance maturity Level ")
                    .append(m.getLevel()).append(" (").append(m.getLabel()).append("): ")
                    .append(m.getDescription().toLowerCase()));
            headline.append(".");
        }
        html.paragraph(headline.toString());

        if (!results.ranked().isEmpty()) {
            html.paragraph("The strongest-rated areas were " + describe(topN(results.ranked(), 3), ctx)
                    + ". The areas requiring the most attention were "
                    + describe(topN(results.rankedAscending(), 3), ctx) + ".");
        }
        if (ctx.findings().isEmpty()) {
            html.paragraph("No findings have been recorded against this evaluation yet.");
        } else {
            html.paragraph(plural(ctx.findings().size(), "finding was", "findings were") + " recorded ("
                    + severityBreakdown(ctx.findings()) + "), supported by "
                    + plural(ctx.recommendations().size(), "recommendation", "recommendations") + " and "
                    + plural(ctx.actions().size(), "corrective action", "corrective actions")
                    + ", which are set out in sections 15 to 18.");
        }
    }

    private void background(ReportHtmlBuilder html, ReportContext ctx) {
        html.section("Background")
                .paragraph(ctx.organization().getName() + " is committed to high standards of corporate governance. "
                        + "The " + frameworkName(ctx) + " expects boards to undertake a formal and rigorous annual "
                        + "evaluation of the performance of the Board, its committees, the Chairman and individual "
                        + "directors.")
                .paragraph("This report sets out the results of the " + ctx.evaluation().getYear()
                        + " evaluation of the " + ctx.board().getName() + ", conducted through the BE-GEMS board "
                        + "evaluation platform.");
    }

    private void objectives(ReportHtmlBuilder html) {
        html.section("Objectives")
                .paragraph("The evaluation was designed to:")
                .bullets(List.of(
                        "assess how effectively the Board discharges its oversight and strategic responsibilities;",
                        "measure governance maturity across each dimension of the governance framework;",
                        "identify strengths to sustain and gaps to address;",
                        "agree recommendations and a time-bound action plan for improvement; and",
                        "establish a baseline for tracking governance improvement year on year."));
    }

    private void scope(ReportHtmlBuilder html, ReportContext ctx, Results results) {
        html.section("Scope")
                .paragraph("The evaluation covered the " + ctx.board().getName() + ", comprising "
                        + plural(ctx.directors().size(), "director", "directors") + " and "
                        + plural(ctx.committees().size(), "board committee", "board committees") + ", for the "
                        + ctx.evaluation().getYear() + " evaluation cycle (evaluation period: "
                        + ReportFormat.period(ctx.evaluation().getStartDate(), ctx.evaluation().getCloseDate())
                        + ").")
                .paragraph("The following governance dimensions were assessed:")
                .bullets(results.dimensionScores().keySet().stream().map(ctx::dimensionName).toList());
    }

    private void methodology(ReportHtmlBuilder html, ReportContext ctx, Results results) {
        long ratedQuestions = ctx.questions().stream().filter(q -> results.questionById().containsKey(q.getId()))
                .count();
        Map<ConfidentialityMode, Long> modes = ctx.respondents().stream()
                .collect(Collectors.groupingBy(EvaluationRespondent::getConfidentialityMode,
                        () -> new EnumMap<>(ConfidentialityMode.class), Collectors.counting()));
        List<String> modeDescriptions = modes.entrySet().stream()
                .map(e -> e.getValue() + " " + ReportFormat.humanize(e.getKey()).toLowerCase())
                .toList();

        html.section("Methodology")
                .paragraph("Directors completed a structured online questionnaire of "
                        + plural(ratedQuestions, "rated statement", "rated statements") + " aligned to the "
                        + frameworkName(ctx) + ". Each statement was rated on a scale of 1 (lowest) to 5 (highest). "
                        + results.invited() + " directors were invited to respond and " + results.submitted()
                        + " submitted a completed questionnaire.")
                .paragraph("Responses were collected on the following confidentiality basis: "
                        + (modeDescriptions.isEmpty() ? "not applicable" : joinWithAnd(modeDescriptions)) + ".")
                .subheading("Scoring approach")
                .bullets(List.of(
                        "Statement score: the average rating given to each statement.",
                        "Dimension score: the average of the statement scores within that dimension (1.00–5.00).",
                        "Overall board score: the sum of each dimension score multiplied by its framework weight.",
                        "Governance maturity: each score is classified against the six maturity levels in section 6.",
                        "Board Governance Effectiveness Index (BGEI): dimension scores are grouped into categories, "
                                + "converted to a percentage and weighted to give a single index from 0% to 100%."));
    }

    private void framework(ReportHtmlBuilder html, ReportContext ctx) {
        html.section("Evaluation Framework")
                .paragraph("The evaluation applied the " + frameworkName(ctx)
                        + ". Dimensions, weights and BGEI categories were as follows:");
        List<List<String>> rows = ctx.dimensions().stream()
                .map(d -> List.of(d.getName(), percent(d.getDefaultWeightPct()), nullToEmpty(d.getBgeiCategory())))
                .toList();
        BigDecimal totalWeight = ctx.dimensions().stream().map(Dimension::getDefaultWeightPct)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        html.table(List.of(Column.text("Dimension"), Column.number("Weight"), Column.text("BGEI category")), rows,
                List.of("Total", percent(totalWeight), ""));

        html.subheading("Governance maturity levels");
        html.table(List.of(Column.text("Level"), Column.text("Score range"), Column.text("Description")),
                ctx.maturityLevels().stream()
                        .sorted(Comparator.comparingInt(MaturityLevel::getLevel))
                        .map(m -> List.of(m.getLevel() + " – " + m.getLabel(),
                                score(m.getMinScore()) + "–" + score(m.getMaxScore()), m.getDescription()))
                        .toList());

        html.subheading("BGEI classification");
        html.table(List.of(Column.text("Index"), Column.text("Classification")), ctx.bgeiBands().stream()
                .sorted(Comparator.comparing(BgeiBand::getMinPct).reversed())
                .map(b -> List.of(percent(b.getMinPct()) + " – " + percent(b.getMaxPct()), b.getLabel()))
                .toList());
    }

    private void boardComposition(ReportHtmlBuilder html, ReportContext ctx) {
        html.section("Board Composition");
        if (ctx.directors().isEmpty()) {
            html.note("No directors are recorded for this board.");
            return;
        }
        Map<DirectorClassification, Long> counts = ctx.directors().stream()
                .collect(Collectors.groupingBy(Director::getClassification,
                        () -> new EnumMap<>(DirectorClassification.class), Collectors.counting()));
        long independent = counts.getOrDefault(DirectorClassification.INDEPENDENT_NON_EXECUTIVE_DIRECTOR, 0L);
        long nonExecutive = independent + counts.getOrDefault(DirectorClassification.NON_EXECUTIVE_DIRECTOR, 0L)
                + counts.getOrDefault(DirectorClassification.CHAIRMAN, 0L);

        html.paragraph("The Board comprised " + plural(ctx.directors().size(), "director", "directors")
                + " at the time of the evaluation: "
                + joinWithAnd(counts.entrySet().stream()
                        .map(e -> e.getValue() + " " + ReportFormat.classification(e.getKey()))
                        .toList())
                + ". Non-executive directors (including the Chairman) made up " + nonExecutive + " of "
                + ctx.directors().size() + ", of whom " + independent + " "
                + (independent == 1 ? "is" : "are") + " independent.");

        Map<UUID, List<String>> committeesByDirector = committeesByDirector(ctx);
        List<List<String>> rows = ctx.directors().stream()
                .sorted(Comparator.comparing(Director::getClassification).thenComparing(Director::getName))
                .map(d -> List.of(d.getName(), ReportFormat.classification(d.getClassification()),
                        nullToEmpty(date(d.getAppointmentDate())), nullToEmpty(date(d.getTermExpirationDate())),
                        String.join(", ", committeesByDirector.getOrDefault(d.getId(), List.of()))))
                .toList();
        html.table(List.of(Column.text("Director"), Column.text("Classification"), Column.text("Appointed"),
                Column.text("Term expires"), Column.text("Committees")), rows);
        skillsMatrix(html, ctx);
    }

    /** Coverage is reported by competency only — individual directors' ratings are not named here. */
    private void skillsMatrix(ReportHtmlBuilder html, ReportContext ctx) {
        html.subheading("Board Skills Matrix");
        List<SkillRow> skills = ctx.skills().skills();
        List<SkillRow> assessed = skills.stream().filter(s -> s.coverage() != SkillCoverage.NOT_ASSESSED).toList();
        if (assessed.isEmpty()) {
            html.note("The Board skills matrix has not been completed, so no skills analysis is included.");
            return;
        }
        Map<SkillCoverage, List<String>> byCoverage = assessed.stream()
                .collect(Collectors.groupingBy(SkillRow::coverage, () -> new EnumMap<>(SkillCoverage.class),
                        Collectors.mapping(SkillRow::name, Collectors.toList())));
        List<String> parts = new ArrayList<>();
        addCount(parts, byCoverage, SkillCoverage.COVERED, "adequately covered", "adequately covered");
        addCount(parts, byCoverage, SkillCoverage.UNDERREPRESENTED, "underrepresented", "underrepresented");
        addCount(parts, byCoverage, SkillCoverage.SINGLE_PERSON_DEPENDENCY, "single-person dependency",
                "single-person dependencies");
        addCount(parts, byCoverage, SkillCoverage.CRITICAL_GAP, "critical gap", "critical gaps");
        html.paragraph("Each director was rated from 1 (Basic) to 5 (Expert) against the competencies the Board "
                + "requires, and each competency carries a Board requirement of Low, Medium or High. Of the "
                + plural(assessed.size(), "competency", "competencies") + " assessed: " + joinWithAnd(parts) + "."
                + (assessed.size() < skills.size()
                        ? " " + plural(skills.size() - assessed.size(), "competency has", "competencies have")
                                + " not yet been rated."
                        : ""));

        List<String> concerns = new ArrayList<>();
        concern(concerns, byCoverage, SkillCoverage.CRITICAL_GAP,
                "Critical gaps (High requirement, no director rated Advanced or above): ");
        concern(concerns, byCoverage, SkillCoverage.SINGLE_PERSON_DEPENDENCY,
                "Single-person dependencies (High requirement, only one director rated Advanced or above): ");
        concern(concerns, byCoverage, SkillCoverage.UNDERREPRESENTED, "Underrepresented: ");
        List<String> future = skills.stream()
                .filter(SkillRow::futureFocus)
                .map(s -> s.name() + " (" + ReportFormat.skillCoverage(s.coverage()).toLowerCase(Locale.ROOT) + ")")
                .toList();
        if (!future.isEmpty()) {
            concerns.add("Future skills requirements: " + joinWithAnd(future) + ".");
        }
        if (!concerns.isEmpty()) {
            html.bullets(concerns);
        }

        html.table(List.of(Column.text("Competency"), Column.text("Board requirement"), Column.number("Average"),
                Column.number("Directors rated 4+"), Column.text("Coverage")),
                skills.stream()
                        .map(s -> List.of(s.name() + (s.futureFocus() ? " (future)" : ""),
                                ReportFormat.humanize(s.requiredLevel()),
                                s.average() == null ? "—" : score(BigDecimal.valueOf(s.average())),
                                String.valueOf(s.proficientCount()), ReportFormat.skillCoverage(s.coverage())))
                        .toList());
    }

    private static void addCount(List<String> parts, Map<SkillCoverage, List<String>> byCoverage,
            SkillCoverage coverage, String singular, String pluralForm) {
        List<String> names = byCoverage.getOrDefault(coverage, List.of());
        if (!names.isEmpty()) {
            parts.add(plural(names.size(), singular, pluralForm));
        }
    }

    private static void concern(List<String> concerns, Map<SkillCoverage, List<String>> byCoverage,
            SkillCoverage coverage, String label) {
        List<String> names = byCoverage.getOrDefault(coverage, List.of());
        if (!names.isEmpty()) {
            concerns.add(label + joinWithAnd(names) + ".");
        }
    }

    private void effectivenessResults(ReportHtmlBuilder html, ReportContext ctx, Results results) {
        html.section("Board Effectiveness Results");
        Map<UUID, Dimension> dimensions = ctx.dimensionsById();
        List<List<String>> rows = results.dimensionScores().values().stream()
                .map(s -> {
                    Dimension d = dimensions.get(s.getDimensionId());
                    return List.of(ctx.dimensionName(s.getDimensionId()), score(s.getRawScore()),
                            d == null ? "" : percent(d.getDefaultWeightPct()), nullToEmpty(score(s.getWeightedScore())),
                            maturityText(ctx, s.getMaturityLevel()));
                })
                .toList();
        html.table(List.of(Column.text("Dimension"), Column.number("Score"), Column.number("Weight"),
                Column.number("Weighted"), Column.text("Maturity")), rows,
                results.boardScore() == null ? null
                        : List.of("Overall board score", "", "", score(results.boardScore()),
                                nullToEmpty(results.boardMaturityLabel())));

        List<EvaluationScore> categories = ctx.scoresOf(ScoreScopeType.BGEI_CATEGORY);
        if (!categories.isEmpty()) {
            html.subheading("Board Governance Effectiveness Index");
            List<String> categoryOrder = ctx.dimensions().stream().map(Dimension::getBgeiCategory).distinct().toList();
            List<List<String>> categoryRows = categories.stream()
                    .sorted(Comparator.comparingInt(c -> categoryOrder.indexOf(c.getBgeiCategory())))
                    .map(c -> List.of(c.getBgeiCategory(), score(c.getRawScore()),
                            percent(c.getRawScore().multiply(BigDecimal.valueOf(20))),
                            percent(c.getWeightedScore())))
                    .toList();
            html.table(List.of(Column.text("Category"), Column.number("Score (1–5)"), Column.number("Score %"),
                    Column.number("Contribution")), categoryRows,
                    List.of("BGEI", "", "", nullToEmpty(percent(results.bgeiPct()))));
            if (results.bgeiBand() != null) {
                html.paragraph("The Board's overall BGEI of " + percent(results.bgeiPct()) + " is classified as "
                        + results.bgeiBand() + ".");
            }
        }
    }

    private void committeeResults(ReportHtmlBuilder html, ReportContext ctx, Results results) {
        html.section("Committee Results");
        dimensionNarrative(html, ctx, results, "COMMITTEES", "committee effectiveness");
        dimensionNarrative(html, ctx, results, "AUDIT_INTERNAL_CONTROLS", "audit and internal controls oversight");
        if (!ctx.committees().isEmpty()) {
            html.subheading("Board committees");
            Map<UUID, Director> directors = ctx.directorsById();
            Map<UUID, List<CommitteeMember>> membersByCommittee = ctx.committeeMembers().stream()
                    .collect(Collectors.groupingBy(CommitteeMember::getCommitteeId));
            List<List<String>> rows = ctx.committees().stream()
                    .sorted(Comparator.comparing(Committee::getName))
                    .map(c -> {
                        List<CommitteeMember> members = membersByCommittee.getOrDefault(c.getId(), List.of());
                        String chair = members.stream().filter(m -> m.getRole() == CommitteeMemberRole.CHAIR)
                                .map(m -> directorName(directors, m.getDirectorId())).findFirst().orElse("");
                        return List.of(c.getName(), chair, String.valueOf(members.size()),
                                nullToEmpty(c.getMeetingFrequency()));
                    })
                    .toList();
            html.table(List.of(Column.text("Committee"), Column.text("Chair"), Column.number("Members"),
                    Column.text("Meeting frequency")), rows);
        }
        html.note("Committee results are drawn from the Board questionnaire; separate committee-level "
                + "questionnaires were not part of this evaluation cycle.");
    }

    private void chairmanEvaluation(ReportHtmlBuilder html, ReportContext ctx, Results results) {
        html.section("Chairman Evaluation");
        ctx.directors().stream().filter(d -> d.getClassification() == DirectorClassification.CHAIRMAN).findFirst()
                .ifPresent(chairman -> html.paragraph("The Board was chaired by " + chairman.getName()
                        + " during the period under review."));
        dimensionNarrative(html, ctx, results, "BOARD_LEADERSHIP", "board leadership");
        html.note("The Chairman's effectiveness is assessed through the Board Leadership statements of the Board "
                + "questionnaire.");
    }

    private void directorEvaluation(ReportHtmlBuilder html, ReportContext ctx, Results results) {
        html.section("Director Evaluation");
        dimensionNarrative(html, ctx, results, "DIRECTOR_PERFORMANCE", "director performance");
        List<BigDecimal> peerScores = ctx.peerDirectorScores();
        if (peerScores.isEmpty()) {
            html.paragraph("No director peer-to-peer evaluations have been scored for " + ctx.evaluation().getYear()
                    + ".");
        } else {
            html.paragraph(plural(peerScores.size(), "director peer-to-peer evaluation was",
                    "director peer-to-peer evaluations were") + " completed for " + ctx.evaluation().getYear()
                    + ", with an average overall director score of "
                    + outOfFive(ResponseStatistics.average(peerScores)) + ".");
        }
        html.note("Individual director results are reported separately in confidential individual director "
                + "reports and are not reproduced here.");
    }

    private void governanceMaturity(ReportHtmlBuilder html, ReportContext ctx, Results results) {
        html.section("Governance Maturity");
        results.boardMaturity().ifPresent(m -> html.paragraph("Overall, the Board is operating at governance "
                + "maturity Level " + m.getLevel() + " (" + m.getLabel() + "): " + m.getDescription() + "."));
        Map<Integer, List<String>> dimensionsByLevel = new HashMap<>();
        results.dimensionScores().values().forEach(s -> dimensionsByLevel
                .computeIfAbsent(s.getMaturityLevel(), k -> new ArrayList<>())
                .add(ctx.dimensionName(s.getDimensionId())));
        List<List<String>> rows = ctx.maturityLevels().stream()
                .sorted(Comparator.comparingInt(MaturityLevel::getLevel).reversed())
                .map(m -> {
                    List<String> names = dimensionsByLevel.getOrDefault(m.getLevel(), List.of());
                    return List.of(m.getLevel() + " – " + m.getLabel(), String.valueOf(names.size()),
                            String.join(", ", names));
                })
                .toList();
        html.table(List.of(Column.text("Maturity level"), Column.number("Dimensions"), Column.text("Dimensions at this level")),
                rows);
    }

    private void regulatoryBenchmark(ReportHtmlBuilder html, ReportContext ctx) {
        html.section("Regulatory Benchmark");
        BenchmarkComparison comparison = BenchmarkAnalysis.compare(ctx.dimensions(), ctx.scores(), ctx.benchmarks());
        if (comparison.rows().isEmpty()) {
            html.note("No scores are available to compare against the benchmarks.");
            return;
        }
        Benchmarks.Target defaultTarget = ctx.benchmarks().defaultDimension();
        boolean allDefault = comparison.rows().stream().allMatch(BenchmarkComparisonRow::defaultBenchmark);
        html.paragraph(allDefault
                ? "Each dimension is compared with the BE-GEMS default benchmark of a score of at least "
                        + score(defaultTarget.value()) + " (" + defaultTarget.source().replace("BE-GEMS default: ", "")
                        + "), the point at which governance practices are formally documented and consistently "
                        + "applied in line with the " + frameworkName(ctx) + ". The BGEI is compared with "
                        + percent(ctx.benchmarks().bgei().value()) + ", the lower bound of the Effective band."
                : "Each area is compared with the benchmark " + ctx.organization().getName() + " has set for it, "
                        + "and the basis for each benchmark is shown in the table. Where no benchmark has been set, "
                        + "the BE-GEMS default applies.");

        List<BenchmarkComparisonRow> below = comparison.rows().stream()
                .filter(BenchmarkComparisonRow::belowBenchmark)
                .toList();
        html.paragraph(below.isEmpty() ? "Every area met or exceeded its benchmark."
                : plural(below.size(), "area falls", "areas fall") + " below the benchmark (negative variance): "
                        + joinWithAnd(below.stream()
                                .map(r -> r.measure() + " (" + benchmarkValue(r, r.variance(), true) + ")")
                                .toList())
                        + ".");
        html.table(List.of(Column.text("Measure"), Column.number("Actual"), Column.number("Benchmark"),
                Column.number("Variance"), Column.text("Status"), Column.text("Basis")),
                comparison.rows().stream()
                        .map(r -> List.of(r.measure(), benchmarkValue(r, r.actual(), false),
                                benchmarkValue(r, r.benchmark(), false), benchmarkValue(r, r.variance(), true),
                                r.belowBenchmark() ? "Below benchmark" : "Meets benchmark",
                                nullToEmpty(r.source())))
                        .toList());

        List<Finding> referenced = ctx.findingsInReportOrder().stream()
                .filter(f -> f.getRegulatoryReference() != null && !f.getRegulatoryReference().isBlank())
                .toList();
        if (!referenced.isEmpty()) {
            html.subheading("Findings with regulatory references");
            Map<UUID, String> refs = findingRefs(ctx);
            html.table(List.of(Column.text("Ref"), Column.text("Regulatory reference"), Column.text("Finding"),
                    Column.text("Severity")),
                    referenced.stream().map(f -> List.of(refs.get(f.getId()), f.getRegulatoryReference(),
                            f.getDescription(), ReportFormat.humanize(f.getSeverity()))).toList());
        }
    }

    /** BGEI rows are percentages; dimension rows are scores out of 5. */
    private static String benchmarkValue(BenchmarkComparisonRow row, BigDecimal value, boolean signed) {
        String text = row.dimensionId() == null ? percent(value) : score(value);
        return signed && value.signum() > 0 ? "+" + text : text;
    }

    private void keyStrengths(ReportHtmlBuilder html, Results results) {
        html.section("Key Strengths");
        List<EvaluationScore> strengths = results.ranked().stream()
                .filter(s -> s.getRawScore().compareTo(STRENGTH_THRESHOLD) >= 0)
                .limit(HIGHLIGHT_LIMIT).toList();
        if (strengths.isEmpty()) {
            html.paragraph("No dimension reached the Managed level (" + score(STRENGTH_THRESHOLD)
                    + "). The Board's relative strengths were:");
            strengths = topN(results.ranked(), 3);
        } else {
            html.paragraph("The following dimensions were rated at the Managed level or above:");
        }
        html.bullets(strengths.stream().map(results::describeScore).toList());
        if (!results.questionResults().isEmpty()) {
            html.subheading("Highest-rated statements");
            html.table(statementColumns(), results.questionResults().stream()
                    .sorted(Comparator.comparing(QuestionResult::average).reversed())
                    .limit(HIGHLIGHT_LIMIT).map(results::statementRow).toList());
        }
    }

    private void keyFindings(ReportHtmlBuilder html, ReportContext ctx) {
        html.section("Key Findings");
        if (ctx.findings().isEmpty()) {
            html.note("No findings have been recorded for this evaluation.");
            return;
        }
        Map<UUID, String> refs = findingRefs(ctx);
        for (Finding finding : ctx.findingsInReportOrder()) {
            html.subheading(refs.get(finding.getId()) + ". " + finding.getDescription());
            html.keyValues(List.of(
                    new String[] {"Severity", ReportFormat.humanize(finding.getSeverity())},
                    new String[] {"Dimension", ctx.dimensionName(finding.getDimensionId())},
                    new String[] {"Evidence", finding.getEvidence()},
                    new String[] {"Regulatory reference", finding.getRegulatoryReference()},
                    new String[] {"Root cause", finding.getRootCause()},
                    new String[] {"Risk implication", finding.getRiskImplication()}));
        }
    }

    private void areasForImprovement(ReportHtmlBuilder html, Results results) {
        html.section("Areas for Improvement");
        List<EvaluationScore> weaker = results.rankedAscending().stream()
                .filter(s -> s.getRawScore().compareTo(STRENGTH_THRESHOLD) < 0)
                .limit(HIGHLIGHT_LIMIT).toList();
        if (weaker.isEmpty()) {
            html.paragraph("Every dimension was rated at the Managed level or above. The lowest-rated dimensions, "
                    + "which offer the most room for further improvement, were:");
            weaker = topN(results.rankedAscending(), 3);
        } else {
            html.paragraph("The following dimensions were rated below the Managed level ("
                    + score(STRENGTH_THRESHOLD) + ") and should be prioritised:");
        }
        html.bullets(weaker.stream().map(results::describeScore).toList());
        if (!results.questionResults().isEmpty()) {
            html.subheading("Lowest-rated statements");
            html.table(statementColumns(), results.questionResults().stream()
                    .sorted(Comparator.comparing(QuestionResult::average))
                    .limit(HIGHLIGHT_LIMIT).map(results::statementRow).toList());
        }
    }

    private void recommendations(ReportHtmlBuilder html, ReportContext ctx) {
        html.section("Recommendations");
        if (ctx.recommendations().isEmpty()) {
            html.note("No recommendations have been recorded for this evaluation.");
            return;
        }
        Map<UUID, String> refs = findingRefs(ctx);
        List<List<String>> rows = ctx.recommendations().stream()
                .sorted(Comparator.comparing((Recommendation r) -> refOrder(refs, r.getFindingId()))
                        .thenComparing(Recommendation::getPriority))
                .map(r -> List.of(nullToEmpty(refs.get(r.getFindingId())), r.getRecommendedAction(),
                        nullToEmpty(r.getResponsiblePerson()), nullToEmpty(r.getCommitteeResponsible()),
                        nullToEmpty(date(r.getTargetDate())), ReportFormat.humanize(r.getPriority()),
                        ReportFormat.humanize(r.getStatus())))
                .toList();
        html.table(List.of(Column.text("Finding"), Column.text("Recommended action"), Column.text("Responsible"),
                Column.text("Committee"), Column.text("Target date"), Column.text("Priority"),
                Column.text("Status")), rows);
    }

    private void actionPlan(ReportHtmlBuilder html, ReportContext ctx) {
        html.section("Action Plan");
        if (ctx.actions().isEmpty()) {
            html.note("No corrective actions have been recorded for this evaluation.");
            return;
        }
        Map<UUID, String> refs = findingRefs(ctx);
        LocalDate today = ctx.generatedOn();
        long overdue = ctx.actions().stream().filter(a -> isOverdue(a, today)).count();
        long completed = ctx.actions().stream().filter(a -> a.getStatus() == ActionStatus.COMPLETED).count();
        html.paragraph(completed + " of " + ctx.actions().size() + " corrective actions are complete"
                + (overdue > 0 ? " and " + overdue + " " + (overdue == 1 ? "is" : "are") + " overdue." : "."));
        List<List<String>> rows = ctx.actions().stream()
                .sorted(Comparator.comparing((CorrectiveAction a) -> refOrder(refs, a.getFindingId()))
                        .thenComparing(CorrectiveAction::getDueDate, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(a -> List.of(nullToEmpty(refs.get(a.getFindingId())), a.getDescription(),
                        nullToEmpty(a.getOwner()), nullToEmpty(a.getApprover()), nullToEmpty(date(a.getDueDate())),
                        ReportFormat.humanize(a.getStatus()) + (isOverdue(a, today) ? " (overdue)" : "")))
                .toList();
        html.table(List.of(Column.text("Finding"), Column.text("Action"), Column.text("Owner"),
                Column.text("Approver"), Column.text("Due date"), Column.text("Status")), rows);
    }

    private void conclusion(ReportHtmlBuilder html, ReportContext ctx, Results results) {
        html.section("Conclusion");
        StringBuilder text = new StringBuilder("Overall, the ").append(ctx.board().getName());
        results.boardMaturity().ifPresentOrElse(
                m -> text.append(" is operating at governance maturity Level ").append(m.getLevel()).append(" (")
                        .append(m.getLabel()).append(")"),
                () -> text.append(" has completed its ").append(ctx.evaluation().getYear()).append(" evaluation"));
        if (results.bgeiPct() != null) {
            text.append(", with a Board Governance Effectiveness Index of ").append(percent(results.bgeiPct()))
                    .append(" (").append(results.bgeiBand()).append(")");
        }
        text.append(".");
        html.paragraph(text.toString());
        if (ctx.recommendations().isEmpty() && ctx.actions().isEmpty()) {
            html.paragraph("Sustaining the strengths identified in this report and addressing the lower-rated areas "
                    + "will be central to strengthening governance ahead of the next evaluation.");
        } else {
            html.paragraph("Timely implementation of the "
                    + plural(ctx.recommendations().size(), "recommendation", "recommendations") + " and "
                    + plural(ctx.actions().size(), "corrective action", "corrective actions")
                    + " in this report, tracked through the action plan, will be central to strengthening "
                    + "governance ahead of the next evaluation.");
        }
        html.paragraph("Progress against the action plan should be reviewed by the Board during the year and "
                + "re-assessed at the next annual evaluation.");
    }

    private void appendices(ReportHtmlBuilder html, ReportContext ctx, Results results) {
        html.pageBreak().heading("Appendices");

        html.subheading("Appendix A — Statement-level results");
        if (results.questionResults().isEmpty()) {
            html.note("No rated responses were recorded.");
        }
        Map<UUID, List<QuestionResult>> byDimension = results.questionResults().stream()
                .collect(Collectors.groupingBy(r -> r.question().getDimensionId(), LinkedHashMap::new,
                        Collectors.toList()));
        for (Dimension dimension : ctx.dimensions()) {
            List<QuestionResult> rows = byDimension.get(dimension.getId());
            if (rows == null) {
                continue;
            }
            html.paragraph(dimension.getName());
            html.table(List.of(Column.text("Statement"), Column.number("Responses"), Column.number("Average")),
                    rows.stream().map(r -> List.of(r.question().getText(), String.valueOf(r.responseCount()),
                            score(r.average()))).toList());
        }

        html.subheading("Appendix B — Respondent participation");
        Map<RespondentStatus, Long> byStatus = ctx.respondents().stream()
                .collect(Collectors.groupingBy(EvaluationRespondent::getStatus,
                        () -> new EnumMap<>(RespondentStatus.class), Collectors.counting()));
        html.table(List.of(Column.text("Status"), Column.number("Respondents")),
                Arrays.stream(RespondentStatus.values())
                        .map(s -> List.of(ReportFormat.humanize(s), String.valueOf(byStatus.getOrDefault(s, 0L))))
                        .toList(),
                List.of("Total invited", String.valueOf(ctx.respondents().size())));

        html.subheading("Appendix C — Respondent comments");
        Map<Question, List<String>> comments = ResponseStatistics.comments(ctx.questions(), ctx.responses());
        if (comments.isEmpty()) {
            html.note("No written comments were provided.");
        }
        comments.forEach((question, texts) -> {
            html.paragraph(question.getText());
            html.quotes(texts);
        });
    }

    private void dimensionNarrative(ReportHtmlBuilder html, ReportContext ctx, Results results, String code,
            String subject) {
        Optional<Dimension> dimension = ctx.dimensionByCode(code);
        if (dimension.isEmpty()) {
            return;
        }
        EvaluationScore dimensionScore = results.dimensionScores().get(dimension.get().getId());
        html.subheading(dimension.get().getName());
        if (dimensionScore == null) {
            html.note("No ratings were recorded for " + subject + ".");
            return;
        }
        html.paragraph("Directors rated " + subject + " at " + outOfFive(dimensionScore.getRawScore())
                + ", which corresponds to " + maturityText(ctx, dimensionScore.getMaturityLevel()) + ".");
        List<QuestionResult> statements = results.questionResults().stream()
                .filter(r -> r.question().getDimensionId().equals(dimension.get().getId()))
                .toList();
        if (!statements.isEmpty()) {
            html.table(statementColumns(), statements.stream().map(results::statementRow).toList());
        }
    }

    private static List<Column> statementColumns() {
        return List.of(Column.text("Statement"), Column.text("Dimension"), Column.number("Average"));
    }

    private static Map<UUID, List<String>> committeesByDirector(ReportContext ctx) {
        Map<UUID, String> committeeNames = ctx.committees().stream()
                .collect(Collectors.toMap(Committee::getId, Committee::getName));
        Map<UUID, List<String>> result = new HashMap<>();
        for (CommitteeMember member : ctx.committeeMembers()) {
            String name = committeeNames.get(member.getCommitteeId());
            if (name != null) {
                result.computeIfAbsent(member.getDirectorId(), k -> new ArrayList<>())
                        .add(member.getRole() == CommitteeMemberRole.CHAIR ? name + " (Chair)" : name);
            }
        }
        result.values().forEach(names -> names.sort(String::compareTo));
        return result;
    }

    static Map<UUID, String> findingRefs(ReportContext ctx) {
        Map<UUID, String> refs = new LinkedHashMap<>();
        List<Finding> ordered = ctx.findingsInReportOrder();
        for (int i = 0; i < ordered.size(); i++) {
            refs.put(ordered.get(i).getId(), "F" + (i + 1));
        }
        return refs;
    }

    private static int refOrder(Map<UUID, String> refs, UUID findingId) {
        String ref = refs.get(findingId);
        return ref == null ? Integer.MAX_VALUE : Integer.parseInt(ref.substring(1));
    }

    private static boolean isOverdue(CorrectiveAction action, LocalDate today) {
        return action.getStatus() != ActionStatus.COMPLETED && action.getDueDate() != null
                && action.getDueDate().isBefore(today);
    }

    private static String severityBreakdown(List<Finding> findings) {
        Map<FindingSeverity, Long> counts = findings.stream()
                .collect(Collectors.groupingBy(Finding::getSeverity, () -> new EnumMap<>(FindingSeverity.class),
                        Collectors.counting()));
        return joinWithAnd(counts.entrySet().stream()
                .map(e -> e.getValue() + " " + ReportFormat.humanize(e.getKey()).toLowerCase())
                .toList());
    }

    private static String describe(List<EvaluationScore> scores, ReportContext ctx) {
        return joinWithAnd(scores.stream()
                .map(s -> ctx.dimensionName(s.getDimensionId()) + " (" + score(s.getRawScore()) + ")")
                .toList());
    }

    private static <T> List<T> topN(List<T> items, int n) {
        return items.subList(0, Math.min(n, items.size()));
    }

    private static String maturityText(ReportContext ctx, Integer level) {
        return ctx.maturityLevel(level).map(m -> "Level " + m.getLevel() + " – " + m.getLabel()).orElse("");
    }

    private static String directorName(Map<UUID, Director> directors, UUID directorId) {
        Director director = directors.get(directorId);
        return director == null ? "" : director.getName();
    }

    private static String frameworkName(ReportContext ctx) {
        return ctx.framework().getName() + " (" + ctx.framework().getVersion() + ")";
    }

    private static String percentOrDash(BigDecimal value) {
        return value == null ? "—" : percent(value);
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    /** Persisted scores indexed the ways the sections need them. */
    private static final class Results {

        private final ReportContext ctx;
        private final Map<UUID, EvaluationScore> dimensionScores = new LinkedHashMap<>();
        private final List<QuestionResult> questionResults;
        private final Map<UUID, QuestionResult> questionById;
        private final EvaluationScore boardOverall;
        private final EvaluationScore bgeiOverall;

        Results(ReportContext ctx) {
            this.ctx = ctx;
            Map<UUID, EvaluationScore> byDimension = ctx.scoresOf(ScoreScopeType.DIMENSION).stream()
                    .collect(Collectors.toMap(EvaluationScore::getDimensionId, s -> s));
            ctx.dimensions().forEach(d -> {
                if (byDimension.containsKey(d.getId())) {
                    dimensionScores.put(d.getId(), byDimension.get(d.getId()));
                }
            });
            this.questionResults = ResponseStatistics.questionResults(ctx.questions(), ctx.responses());
            this.questionById = questionResults.stream()
                    .collect(Collectors.toMap(r -> r.question().getId(), r -> r));
            this.boardOverall = ctx.scoreOf(ScoreScopeType.BOARD_OVERALL).orElse(null);
            this.bgeiOverall = ctx.scoreOf(ScoreScopeType.BGEI_OVERALL).orElse(null);
        }

        Map<UUID, EvaluationScore> dimensionScores() {
            return dimensionScores;
        }

        List<QuestionResult> questionResults() {
            return questionResults;
        }

        Map<UUID, QuestionResult> questionById() {
            return questionById;
        }

        List<EvaluationScore> ranked() {
            return dimensionScores.values().stream()
                    .sorted(Comparator.comparing(EvaluationScore::getRawScore).reversed())
                    .toList();
        }

        List<EvaluationScore> rankedAscending() {
            return dimensionScores.values().stream()
                    .sorted(Comparator.comparing(EvaluationScore::getRawScore))
                    .toList();
        }

        BigDecimal boardScore() {
            return boardOverall == null ? null : boardOverall.getRawScore();
        }

        Optional<MaturityLevel> boardMaturity() {
            return boardOverall == null ? Optional.empty() : ctx.maturityLevel(boardOverall.getMaturityLevel());
        }

        String boardMaturityLabel() {
            return boardMaturity().map(m -> "Level " + m.getLevel() + " – " + m.getLabel()).orElse(null);
        }

        BigDecimal bgeiPct() {
            return bgeiOverall == null ? null : bgeiOverall.getWeightedScore();
        }

        String bgeiBand() {
            return bgeiOverall == null ? null : bgeiOverall.getBgeiBandLabel();
        }

        long invited() {
            return ctx.respondents().size();
        }

        long submitted() {
            return ctx.respondents().stream().filter(r -> r.getStatus() == RespondentStatus.SUBMITTED).count();
        }

        String describeScore(EvaluationScore s) {
            return ctx.dimensionName(s.getDimensionId()) + ": " + outOfFive(s.getRawScore()) + " ("
                    + maturityText(ctx, s.getMaturityLevel()) + ")";
        }

        List<String> statementRow(QuestionResult r) {
            return List.of(r.question().getText(), ctx.dimensionName(r.question().getDimensionId()),
                    score(r.average()));
        }
    }
}
