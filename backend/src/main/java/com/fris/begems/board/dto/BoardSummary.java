package com.fris.begems.board.dto;

import com.fris.begems.board.Board;
import java.time.LocalDate;
import java.util.UUID;

public record BoardSummary(UUID id, String name, LocalDate effectiveDate, String notes) {

    public static BoardSummary from(Board board) {
        return new BoardSummary(board.getId(), board.getName(), board.getEffectiveDate(), board.getNotes());
    }
}
