package com.fris.begems.report;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Print-ready HTML document builder (board-portal's export pattern, factored out
 * so long reports stay readable). Every text argument is HTML-escaped here, so
 * callers pass raw user data. Numbered sections are collected as they're written
 * and rendered into the contents list wherever {@link #contents()} was called.
 */
public final class ReportHtmlBuilder {

    private static final String CONTENTS_MARKER = "<!--report-contents-->";

    private static final String STYLE = """
            @page{size:A4;margin:18mm 16mm;}
            *{box-sizing:border-box;}
            body{font-family:Georgia,'Times New Roman',serif;max-width:860px;margin:0 auto;padding:1.5rem 1.25rem 3rem;color:#1a1a1a;line-height:1.5;}
            .toolbar{position:sticky;top:0;display:flex;justify-content:flex-end;padding:0.5rem 0;background:#fff;}
            .toolbar button{font:600 0.9rem system-ui,sans-serif;padding:0.5rem 1rem;border:0;border-radius:6px;background:#0f2a4a;color:#fff;cursor:pointer;}
            .cover{min-height:min(88vh,900px);display:flex;flex-direction:column;justify-content:center;border-left:6px solid #0f2a4a;padding-left:2rem;}
            .cover .marking{font:700 0.8rem system-ui,sans-serif;letter-spacing:0.12em;text-transform:uppercase;color:#a12a2a;}
            .cover h1{font-size:2.2rem;margin:0.75rem 0 0.25rem;color:#0f2a4a;}
            .cover .subtitle{font-size:1.3rem;margin:0 0 2rem;color:#333;}
            .cover table{width:auto;}
            .cover td{border:0;padding:0.2rem 1.5rem 0.2rem 0;}
            h2{font-size:1.25rem;color:#0f2a4a;margin-top:2.25rem;border-bottom:2px solid #0f2a4a;padding-bottom:0.3rem;break-after:avoid;}
            h3{font-size:1.02rem;margin:1.4rem 0 0.4rem;break-after:avoid;}
            p{margin:0.5rem 0;}
            .note{color:#555;font-style:italic;}
            ol.contents{columns:2;column-gap:2rem;padding-left:1.25rem;}
            ol.contents a{color:inherit;text-decoration:none;}
            table{width:100%;border-collapse:collapse;margin:0.5rem 0 1rem;}
            th,td{text-align:left;vertical-align:top;padding:0.4rem 0.55rem;border-bottom:1px solid #ddd;font-size:0.92rem;}
            th{color:#444;font-weight:600;background:#f3f5f8;}
            td.num,th.num{text-align:right;white-space:nowrap;}
            tr{break-inside:avoid;}
            tr.total td{font-weight:700;border-top:2px solid #0f2a4a;}
            table.kv th{width:32%;background:none;}
            .figures{display:grid;grid-template-columns:repeat(auto-fit,minmax(170px,1fr));gap:0.75rem;margin:0.75rem 0 1rem;}
            .figure{border:1px solid #d5dbe3;border-radius:6px;padding:0.75rem;break-inside:avoid;}
            .figure .label{font:600 0.75rem system-ui,sans-serif;text-transform:uppercase;letter-spacing:0.05em;color:#555;}
            .figure .value{font-size:1.5rem;font-weight:700;color:#0f2a4a;}
            .figure .caption{font-size:0.85rem;color:#444;}
            blockquote{margin:0.5rem 0;padding:0.4rem 0.9rem;border-left:3px solid #c5ccd6;color:#333;white-space:pre-wrap;}
            .page-break{break-after:page;}
            footer{margin-top:3rem;font-size:0.8rem;color:#777;border-top:1px solid #ddd;padding-top:0.5rem;}
            @media print{body{max-width:none;padding:0;}.toolbar{display:none;}.cover{min-height:240mm;}
            th{-webkit-print-color-adjust:exact;print-color-adjust:exact;}}
            """;

    private final StringBuilder body = new StringBuilder();
    private final List<String> sectionHeadings = new ArrayList<>();
    private final String title;

    public ReportHtmlBuilder(String title) {
        this.title = title;
    }

    public ReportHtmlBuilder cover(String marking, String heading, String subtitle, List<String[]> details) {
        body.append("<section class=\"cover\"><div class=\"marking\">").append(escape(marking)).append("</div>")
                .append("<h1>").append(escape(heading)).append("</h1>")
                .append("<p class=\"subtitle\">").append(escape(subtitle)).append("</p><table>");
        for (String[] detail : details) {
            body.append("<tr><td><strong>").append(escape(detail[0])).append("</strong></td><td>")
                    .append(escape(detail[1])).append("</td></tr>");
        }
        body.append("</table></section><div class=\"page-break\"></div>");
        return this;
    }

    /** A heading outside the numbered sequence (confidentiality statement, appendices). */
    public ReportHtmlBuilder heading(String text) {
        body.append("<h2>").append(escape(text)).append("</h2>");
        return this;
    }

    public ReportHtmlBuilder section(String heading) {
        sectionHeadings.add(heading);
        int number = sectionHeadings.size();
        body.append("<h2 id=\"section-").append(number).append("\">").append(number).append(". ")
                .append(escape(heading)).append("</h2>");
        return this;
    }

    public ReportHtmlBuilder contents() {
        body.append("<h2>Contents</h2>").append(CONTENTS_MARKER).append("<div class=\"page-break\"></div>");
        return this;
    }

    public ReportHtmlBuilder subheading(String text) {
        body.append("<h3>").append(escape(text)).append("</h3>");
        return this;
    }

    public ReportHtmlBuilder paragraph(String text) {
        body.append("<p>").append(escape(text)).append("</p>");
        return this;
    }

    public ReportHtmlBuilder note(String text) {
        body.append("<p class=\"note\">").append(escape(text)).append("</p>");
        return this;
    }

    public ReportHtmlBuilder bullets(List<String> items) {
        body.append("<ul>");
        items.forEach(item -> body.append("<li>").append(escape(item)).append("</li>"));
        body.append("</ul>");
        return this;
    }

    public ReportHtmlBuilder quotes(List<String> items) {
        items.forEach(item -> body.append("<blockquote>").append(escape(item)).append("</blockquote>"));
        return this;
    }

    /** Each figure is {label, value, caption}; caption may be null. */
    public ReportHtmlBuilder figures(List<String[]> figures) {
        body.append("<div class=\"figures\">");
        for (String[] figure : figures) {
            body.append("<div class=\"figure\"><div class=\"label\">").append(escape(figure[0])).append("</div>")
                    .append("<div class=\"value\">").append(escape(figure[1])).append("</div>");
            if (figure.length > 2 && figure[2] != null) {
                body.append("<div class=\"caption\">").append(escape(figure[2])).append("</div>");
            }
            body.append("</div>");
        }
        body.append("</div>");
        return this;
    }

    /** Two-column label/value table; rows whose value is null are skipped. */
    public ReportHtmlBuilder keyValues(List<String[]> rows) {
        body.append("<table class=\"kv\">");
        for (String[] row : rows) {
            if (row[1] == null) {
                continue;
            }
            body.append("<tr><th>").append(escape(row[0])).append("</th><td>").append(escape(row[1]))
                    .append("</td></tr>");
        }
        body.append("</table>");
        return this;
    }

    public ReportHtmlBuilder table(List<Column> columns, List<List<String>> rows) {
        return table(columns, rows, null);
    }

    /** {@code totalRow} may be null; empty or null cells render as an em dash. */
    public ReportHtmlBuilder table(List<Column> columns, List<List<String>> rows, List<String> totalRow) {
        body.append("<table><thead><tr>");
        for (Column column : columns) {
            body.append(column.numeric() ? "<th class=\"num\">" : "<th>").append(escape(column.label()))
                    .append("</th>");
        }
        body.append("</tr></thead><tbody>");
        rows.forEach(row -> appendRow(columns, row, ""));
        if (totalRow != null) {
            appendRow(columns, totalRow, " class=\"total\"");
        }
        body.append("</tbody></table>");
        return this;
    }

    public ReportHtmlBuilder pageBreak() {
        body.append("<div class=\"page-break\"></div>");
        return this;
    }

    public ReportHtmlBuilder footer(String text) {
        body.append("<footer>").append(escape(text)).append("</footer>");
        return this;
    }

    public byte[] toBytes() {
        StringBuilder contents = new StringBuilder("<ol class=\"contents\">");
        for (int i = 0; i < sectionHeadings.size(); i++) {
            contents.append("<li><a href=\"#section-").append(i + 1).append("\">")
                    .append(escape(sectionHeadings.get(i))).append("</a></li>");
        }
        contents.append("</ol>");

        String html = "<!doctype html><html lang=\"en\"><head><meta charset=\"utf-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">"
                + "<title>" + escape(title) + "</title><style>" + STYLE + "</style></head><body>"
                + "<div class=\"toolbar\"><button type=\"button\" onclick=\"window.print()\">Print / Save as PDF</button></div>"
                + body.toString().replace(CONTENTS_MARKER, contents.toString())
                + "</body></html>";
        return html.getBytes(StandardCharsets.UTF_8);
    }

    public static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private void appendRow(List<Column> columns, List<String> row, String rowAttributes) {
        body.append("<tr").append(rowAttributes).append(">");
        for (int i = 0; i < columns.size(); i++) {
            String cell = i < row.size() ? row.get(i) : "";
            body.append(columns.get(i).numeric() ? "<td class=\"num\">" : "<td>")
                    .append(cell == null || cell.isEmpty() ? "—" : escape(cell)).append("</td>");
        }
        body.append("</tr>");
    }

    public record Column(String label, boolean numeric) {

        public static Column text(String label) {
            return new Column(label, false);
        }

        public static Column number(String label) {
            return new Column(label, true);
        }
    }
}
