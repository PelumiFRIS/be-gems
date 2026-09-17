package com.fris.begems.framework.dto;

import com.fris.begems.framework.Framework;
import java.util.List;
import java.util.UUID;

public record FrameworkDetail(UUID id, String code, String name, String version, List<DimensionSummary> dimensions) {

    public static FrameworkDetail from(Framework framework, List<DimensionSummary> dimensions) {
        return new FrameworkDetail(framework.getId(), framework.getCode(), framework.getName(),
                framework.getVersion(), dimensions);
    }
}
