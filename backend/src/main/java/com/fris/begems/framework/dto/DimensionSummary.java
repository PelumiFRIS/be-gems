package com.fris.begems.framework.dto;

import com.fris.begems.framework.Dimension;
import java.math.BigDecimal;
import java.util.UUID;

public record DimensionSummary(UUID id, String code, String name, BigDecimal defaultWeightPct, String bgeiCategory) {

    public static DimensionSummary from(Dimension dimension) {
        return new DimensionSummary(dimension.getId(), dimension.getCode(), dimension.getName(),
                dimension.getDefaultWeightPct(), dimension.getBgeiCategory());
    }
}
