package com.fris.begems.board.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public record CreateBoardRequest(@NotBlank String name, LocalDate effectiveDate, String notes) {
}
