package com.fris.begems.approval.dto;

import com.fris.begems.approval.ReportStage;
import java.util.Arrays;
import java.util.List;

public record StageSummary(ReportStage stage, String label) {

    public static List<StageSummary> all() {
        return Arrays.stream(ReportStage.values()).map(s -> new StageSummary(s, s.label())).toList();
    }
}
