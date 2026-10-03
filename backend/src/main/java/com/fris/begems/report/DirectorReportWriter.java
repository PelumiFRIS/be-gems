package com.fris.begems.report;

import static com.fris.begems.report.ReportFormat.date;
import static com.fris.begems.report.ReportFormat.outOfFive;
import static com.fris.begems.report.ReportFormat.plural;
import static com.fris.begems.report.ReportFormat.score;

import com.fris.begems.committee.Committee;
import com.fris.begems.committee.CommitteeMemberRole;
import com.fris.begems.director.Director;
import com.fris.begems.director.DirectorClassification;
import com.fris.begems.evaluation.EvaluationRespondent;
import com.fris.begems.evaluation.RespondentStatus;
import com.fris.begems.framework.Dimension;
import com.fris.begems.framework.Question;
import com.fris.begems.framework.ResponseType;
import com.fris.begems.recommendation.Recommendation;
import com.fris.begems.report.ReportHtmlBuilder.Column;
import com.fris.begems.report.ResponseStatistics.QuestionResult;
import com.fris.begems.response.Response;
import com.fris.begems.scoring.EvaluationScore;
import com.fris.begems.scoring.ScoreScopeType;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * The confidential individual director report (memo §27), produced from a
 * DIRECTOR_PEER evaluation. Respondents are split into self (the subject
 * answering about themselves), Chairman and peers; each group is shown only
 * as an average and comments are never attributed.
 */
@Component
class DirectorReportWriter {

    private static final BigDecimal STRENGTH_THRESHOLD = new BigDecimal("3.70");
    private static final int HIGHLIGHT_LIMIT = 5;

    byte[] write(ReportContext ctx) {
        Director subject = ctx.directorsById().get(ctx.evaluation().getSubjectDirectorId());
        String name = subject == null ? "Director" : subject.getName();
        Groups groups = new Groups(ctx, subject);
        String org = ctx.organization().getName();

        ReportHtmlBuilder html = new ReportHtmlBuilder(name + " — Individual Director Evaluation Report "
                + ctx.evaluation().getYear());
        html.cover("Strictly confidential — individual report", "Individual Director Evaluation Report", name,
                List.of(
                        new String[] {"Classification",
                                subject == null ? "—" : ReportFormat.classification(subject.getClassification())},
                        new String[] {"Organisation", org},
                        new String[] {"Board", ctx.board().getName()},
                        new String[] {"Evaluation year", String.valueOf(ctx.evaluation().getYear())},
                        new String[] {"Evaluation period", ReportFormat.period(ctx.evaluation().getStartDate(),
                                ctx.evaluation().getCloseDate())},
                        new String[] {"Report generated", date(ctx.generatedOn())}));

        html.heading("Confidentiality Notice")
                .paragraph("This report contains a confidential assessment of " + name + "'s performance as a "
                        + "director of " + org + ". It is intended only for " + name + ", the Chairman and those "
                        + "responsible for the board evaluation process, and must not be copied or shared further.")
                .paragraph("Ratings are reported as group averages (self, peers and Chairman) and written comments "
                        + "are not attributed to the directors who made them.");
        html.contents();

        profile(html, ctx, subject);
        summary(html, ctx, groups, name);
        selfAssessment(html, ctx, groups, name);
        peerAssessment(html, ctx, groups);
        chairmanAssessment(html, ctx, groups, subject);
        competencyAssessment(html, ctx, groups);
        attendance(html, ctx);
        contribution(html, ctx, groups, name);
        strengths(html, ctx, groups);
        developmentAreas(html, ctx, groups);
        trainingRecommendations(html, ctx, groups);
        appendix(html, groups);

        html.footer(name + " · Individual Director Evaluation Report " + ctx.evaluation().getYear() + " · "
                + org + " · Generated " + date(ctx.generatedOn()) + " by BE-GEMS · Strictly confidential");
        return html.toBytes();
    }

    private void profile(ReportHtmlBuilder html, ReportContext ctx, Director subject) {
        html.section("Director Profile");
        if (subject == null) {
            html.note("The director's record is no longer available.");
            return;
        }
        Map<UUID, String> committeeNames = ctx.committees().stream()
                .collect(Collectors.toMap(Committee::getId, Committee::getName));
        List<String> memberships = ctx.committeeMembers().stream()
                .filter(m -> m.getDirectorId().equals(subject.getId()) && committeeNames.containsKey(m.getCommitteeId()))
                .map(m -> committeeNames.get(m.getCommitteeId())
                        + (m.getRole() == CommitteeMemberRole.CHAIR ? " (Chair)" : " (Member)"))
                .sorted()
                .toList();
        html.keyValues(List.of(
                new String[] {"Name", subject.getName()},
                new String[] {"Classification", ReportFormat.classification(subject.getClassification())},
                new String[] {"Appointment date", date(subject.getAppointmentDate())},
                new String[] {"Term expires", date(subject.getTermExpirationDate())},
                new String[] {"Re-election date", date(subject.getReElectionDate())},
                new String[] {"Profession", subject.getProfession()},
                new String[] {"Qualification", subject.getQualification()},
                new String[] {"Committee memberships", memberships.isEmpty() ? "None" : String.join(", ", memberships)}));
    }

    private void summary(ReportHtmlBuilder html, ReportContext ctx, Groups groups, String name) {
        html.section("Summary of Results");
        BigDecimal overall = ctx.scoreOf(ScoreScopeType.DIRECTOR_OVERALL).map(EvaluationScore::getRawScore)
                .orElse(null);
        html.figures(List.of(
                new String[] {"Overall score", outOfFive(overall), ctx.maturityLabelFor(overall)},
                new String[] {"Self-assessment", outOfFive(groups.self().overall()), groups.self().caption()},
                new String[] {"Peer assessment", outOfFive(groups.peers().overall()), groups.peers().caption()},
                new String[] {"Chairman assessment", outOfFive(groups.chairman().overall()),
                        groups.chairman().caption()}));
        if (overall != null) {
            html.paragraph(name + " received an overall score of " + outOfFive(overall) + " across "
                    + plural(ctx.scoresOf(ScoreScopeType.DIMENSION).size(), "dimension", "dimensions")
                    + " of the peer-to-peer questionnaire, based on "
                    + plural(groups.submittedCount(), "submitted response", "submitted responses") + ".");
        }
        if (groups.self().overall() != null && groups.others().overall() != null) {
            BigDecimal gap = groups.self().overall().subtract(groups.others().overall());
            String direction = gap.signum() > 0 ? "higher than" : gap.signum() < 0 ? "lower than" : "in line with";
            html.paragraph("The self-assessment was " + direction + " the average rating given by other directors"
                    + (gap.signum() == 0 ? "." : " (by " + score(gap.abs()) + ")."));
        }
    }

    private void selfAssessment(ReportHtmlBuilder html, ReportContext ctx, Groups groups, String name) {
        html.section("Self-assessment");
        if (groups.self().respondents().isEmpty()) {
            html.note(name + " did not complete a self-assessment in this evaluation.");
            return;
        }
        html.paragraph("The table compares " + name + "'s own ratings with the average rating from other "
                + "directors. A positive difference means the self-rating was higher.");
        List<List<String>> rows = new ArrayList<>();
        for (Dimension dimension : ctx.dimensions()) {
            BigDecimal self = groups.self().dimensions().get(dimension.getId());
            BigDecimal others = groups.others().dimensions().get(dimension.getId());
            if (self == null && others == null) {
                continue;
            }
            String difference = self == null || others == null ? ""
                    : signed(self.subtract(others));
            rows.add(List.of(dimension.getName(), nullToEmpty(score(self)), nullToEmpty(score(others)), difference));
        }
        html.table(List.of(Column.text("Dimension"), Column.number("Self"), Column.number("Other directors"),
                Column.number("Difference")), rows);
        Map<Question, List<String>> reflections = ResponseStatistics.comments(ctx.questions(),
                groups.self().responses());
        if (!reflections.isEmpty()) {
            html.subheading("Self-reflection");
            reflections.forEach((question, texts) -> {
                html.paragraph(question.getText());
                html.quotes(texts);
            });
        }
    }

    private void peerAssessment(ReportHtmlBuilder html, ReportContext ctx, Groups groups) {
        html.section("Peer Assessment");
        if (groups.peers().respondents().isEmpty()) {
            html.note("No peer ratings were received.");
            return;
        }
        html.paragraph("Average ratings from " + plural(groups.peers().respondents().size(), "fellow director",
                "fellow directors") + ". Individual peers are not identified.");
        groupTable(html, ctx, groups.peers());
        if (groups.peers().respondents().size() == 1) {
            html.note("Only one peer response was received, so this view reflects a single assessment.");
        }
    }

    private void chairmanAssessment(ReportHtmlBuilder html, ReportContext ctx, Groups groups, Director subject) {
        html.section("Chairman Assessment");
        if (subject != null && subject.getClassification() == DirectorClassification.CHAIRMAN) {
            html.note("As Chairman, this director is not separately assessed by a Chairman rating; the peer "
                    + "assessment above reflects the Board's view.");
            return;
        }
        if (groups.chairman().respondents().isEmpty()) {
            html.note("The Chairman did not take part as a respondent in this evaluation.");
            return;
        }
        html.paragraph("Ratings given by the Chairman of the Board.");
        groupTable(html, ctx, groups.chairman());
    }

    private void competencyAssessment(ReportHtmlBuilder html, ReportContext ctx, Groups groups) {
        html.section("Competency Assessment");
        html.paragraph("Scores by governance competency, combining every response received.");
        List<List<String>> rows = new ArrayList<>();
        Map<UUID, EvaluationScore> persisted = ctx.scoresOf(ScoreScopeType.DIMENSION).stream()
                .collect(Collectors.toMap(EvaluationScore::getDimensionId, s -> s));
        for (Dimension dimension : ctx.dimensions()) {
            EvaluationScore s = persisted.get(dimension.getId());
            if (s != null) {
                rows.add(List.of(dimension.getName(), score(s.getRawScore()),
                        nullToEmpty(ctx.maturityLabelFor(s.getRawScore()))));
            }
        }
        html.table(List.of(Column.text("Competency"), Column.number("Score"), Column.text("Maturity")), rows);
        if (!groups.all().questionResults().isEmpty()) {
            html.subheading("Statement-level results");
            html.table(List.of(Column.text("Statement"), Column.text("Competency"), Column.number("Responses"),
                    Column.number("Average")),
                    groups.all().questionResults().stream()
                            .map(r -> List.of(r.question().getText(), ctx.dimensionName(r.question().getDimensionId()),
                                    String.valueOf(r.responseCount()), score(r.average())))
                            .toList());
        }
    }

    private void attendance(ReportHtmlBuilder html, ReportContext ctx) {
        html.section("Attendance");
        html.note("Meeting attendance is not recorded in BE-GEMS for this evaluation cycle. Please refer to the "
                + "Company Secretary's attendance register for " + ctx.evaluation().getYear() + ".");
    }

    private void contribution(ReportHtmlBuilder html, ReportContext ctx, Groups groups, String name) {
        html.section("Contribution");
        Optional<Dimension> performance = ctx.dimensionByCode("DIRECTOR_PERFORMANCE");
        if (performance.isEmpty()) {
            html.note("No contribution statements were included in this questionnaire.");
            return;
        }
        BigDecimal dimensionScore = groups.others().dimensions().get(performance.get().getId());
        if (dimensionScore == null) {
            html.note("No contribution ratings were received from other directors.");
            return;
        }
        html.paragraph("Other directors rated " + name + "'s preparation, contribution and challenge at "
                + outOfFive(dimensionScore) + " (" + nullToEmpty(ctx.maturityLabelFor(dimensionScore)) + ").");
        html.table(List.of(Column.text("Statement"), Column.number("Average")),
                groups.others().questionResults().stream()
                        .filter(r -> r.question().getDimensionId().equals(performance.get().getId()))
                        .map(r -> List.of(r.question().getText(), score(r.average())))
                        .toList());
    }

    private void strengths(ReportHtmlBuilder html, ReportContext ctx, Groups groups) {
        html.section("Strengths");
        List<QuestionResult> rated = groups.feedbackSource().questionResults();
        List<QuestionResult> strengths = rated.stream()
                .filter(r -> r.average().compareTo(STRENGTH_THRESHOLD) >= 0)
                .sorted(Comparator.comparing(QuestionResult::average).reversed())
                .limit(HIGHLIGHT_LIMIT).toList();
        if (strengths.isEmpty() && !rated.isEmpty()) {
            html.paragraph("No statement was rated at " + score(STRENGTH_THRESHOLD)
                    + " or above. The highest-rated statements were:");
            strengths = rated.stream().sorted(Comparator.comparing(QuestionResult::average).reversed()).limit(3)
                    .toList();
        }
        statementBullets(html, strengths);
        comments(html, ctx, groups, 0, "What other directors said this director should continue doing");
    }

    private void developmentAreas(ReportHtmlBuilder html, ReportContext ctx, Groups groups) {
        html.section("Development Areas");
        List<QuestionResult> rated = groups.feedbackSource().questionResults();
        List<QuestionResult> weaker = rated.stream()
                .filter(r -> r.average().compareTo(STRENGTH_THRESHOLD) < 0)
                .sorted(Comparator.comparing(QuestionResult::average))
                .limit(HIGHLIGHT_LIMIT).toList();
        if (weaker.isEmpty() && !rated.isEmpty()) {
            html.paragraph("Every statement was rated at " + score(STRENGTH_THRESHOLD)
                    + " or above. The lowest-rated statements, which offer the most room for growth, were:");
            weaker = rated.stream().sorted(Comparator.comparing(QuestionResult::average)).limit(3).toList();
        }
        statementBullets(html, weaker);
        comments(html, ctx, groups, 1, "Suggested improvements from other directors");
    }

    private void trainingRecommendations(ReportHtmlBuilder html, ReportContext ctx, Groups groups) {
        html.section("Training Recommendations");
        boolean anyComments = comments(html, ctx, groups, 2, "Training, briefing and support needs identified");
        List<Recommendation> recommendations = ctx.recommendations();
        if (!recommendations.isEmpty()) {
            Map<UUID, String> refs = BoardReportWriter.findingRefs(ctx);
            html.subheading("Agreed recommendations");
            html.table(List.of(Column.text("Finding"), Column.text("Recommendation"), Column.text("Target date"),
                    Column.text("Status")),
                    recommendations.stream()
                            .map(r -> List.of(nullToEmpty(refs.get(r.getFindingId())), r.getRecommendedAction(),
                                    nullToEmpty(date(r.getTargetDate())), ReportFormat.humanize(r.getStatus())))
                            .toList());
        }
        if (!anyComments && recommendations.isEmpty()) {
            html.note("No specific training recommendations were recorded.");
        }
    }

    private void appendix(ReportHtmlBuilder html, Groups groups) {
        html.pageBreak().heading("Appendix — Respondent participation");
        html.table(List.of(Column.text("Respondent group"), Column.number("Invited"), Column.number("Submitted")),
                List.of(groups.self().participationRow("Self"), groups.peers().participationRow("Peers"),
                        groups.chairman().participationRow("Chairman")));
    }

    private void groupTable(ReportHtmlBuilder html, ReportContext ctx, Group group) {
        List<List<String>> rows = ctx.dimensions().stream()
                .filter(d -> group.dimensions().containsKey(d.getId()))
                .map(d -> {
                    BigDecimal value = group.dimensions().get(d.getId());
                    return List.of(d.getName(), score(value), nullToEmpty(ctx.maturityLabelFor(value)));
                })
                .toList();
        html.table(List.of(Column.text("Dimension"), Column.number("Score"), Column.text("Maturity")), rows,
                List.of("Average", nullToEmpty(score(group.overall())),
                        nullToEmpty(ctx.maturityLabelFor(group.overall()))));
    }

    private void statementBullets(ReportHtmlBuilder html, List<QuestionResult> results) {
        if (results.isEmpty()) {
            html.note("No ratings were received from other directors.");
            return;
        }
        html.bullets(results.stream().map(r -> r.question().getText() + " (" + score(r.average()) + ")").toList());
    }

    /**
     * The seeded peer questionnaire ends with three narrative prompts in this order:
     * what to continue, what to improve, and training/support gaps. Self-reflections
     * are excluded here — they appear in the self-assessment section.
     */
    private boolean comments(ReportHtmlBuilder html, ReportContext ctx, Groups groups, int promptIndex,
            String heading) {
        List<Question> prompts = ctx.questions().stream()
                .filter(q -> q.getResponseType() == ResponseType.NARRATIVE)
                .sorted(Comparator.comparingInt(Question::getDisplayOrder))
                .toList();
        if (promptIndex >= prompts.size()) {
            return false;
        }
        List<String> texts = ResponseStatistics.comments(List.of(prompts.get(promptIndex)),
                groups.others().responses()).values().stream().flatMap(List::stream).toList();
        if (texts.isEmpty()) {
            return false;
        }
        html.subheading(heading);
        html.quotes(texts);
        return true;
    }

    private static String signed(BigDecimal value) {
        return (value.signum() > 0 ? "+" : "") + score(value);
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private record Group(List<EvaluationRespondent> respondents, List<Response> responses,
            List<QuestionResult> questionResults, Map<UUID, BigDecimal> dimensions, BigDecimal overall) {

        static Group of(ReportContext ctx, List<EvaluationRespondent> respondents) {
            List<Response> responses = ctx.responsesFrom(respondents);
            List<QuestionResult> results = ResponseStatistics.questionResults(ctx.questions(), responses);
            Map<UUID, BigDecimal> dimensions = ResponseStatistics.dimensionAverages(results);
            return new Group(respondents, responses, results, dimensions,
                    ResponseStatistics.average(dimensions.values()));
        }

        String caption() {
            if (respondents.isEmpty()) {
                return "not completed";
            }
            return plural(respondents.size(), "respondent", "respondents");
        }

        List<String> participationRow(String label) {
            long submitted = respondents.stream().filter(r -> r.getStatus() == RespondentStatus.SUBMITTED).count();
            return List.of(label, String.valueOf(respondents.size()), String.valueOf(submitted));
        }
    }

    private record Groups(Group all, Group self, Group peers, Group chairman, Group others) {

        Groups(ReportContext ctx, Director subject) {
            this(ctx, subject, ctx.directorsById());
        }

        private Groups(ReportContext ctx, Director subject, Map<UUID, Director> directors) {
            this(Group.of(ctx, ctx.respondents()),
                    Group.of(ctx, ctx.respondents().stream().filter(r -> isSelf(r, subject)).toList()),
                    Group.of(ctx, ctx.respondents().stream()
                            .filter(r -> !isSelf(r, subject) && !isChairman(r, directors)).toList()),
                    Group.of(ctx, ctx.respondents().stream()
                            .filter(r -> !isSelf(r, subject) && isChairman(r, directors)).toList()),
                    Group.of(ctx, ctx.respondents().stream().filter(r -> !isSelf(r, subject)).toList()));
        }

        /** Other directors' views where there are any, otherwise everything received. */
        Group feedbackSource() {
            return others.questionResults().isEmpty() ? all : others;
        }

        long submittedCount() {
            return all.respondents().stream().filter(r -> r.getStatus() == RespondentStatus.SUBMITTED).count();
        }

        private static boolean isSelf(EvaluationRespondent respondent, Director subject) {
            return subject != null && respondent.getDirectorId().equals(subject.getId());
        }

        private static boolean isChairman(EvaluationRespondent respondent, Map<UUID, Director> directors) {
            Director director = directors.get(respondent.getDirectorId());
            return director != null && director.getClassification() == DirectorClassification.CHAIRMAN;
        }
    }
}
