package com.fris.begems.director.dto;

import com.fris.begems.director.Director;
import com.fris.begems.director.DirectorClassification;
import java.time.LocalDate;
import java.util.UUID;

public record DirectorSummary(
        UUID id,
        UUID boardId,
        String name,
        String email,
        DirectorClassification classification,
        LocalDate appointmentDate,
        LocalDate termExpirationDate,
        boolean hasPortalAccess) {

    public static DirectorSummary from(Director director) {
        return new DirectorSummary(
                director.getId(),
                director.getBoardId(),
                director.getName(),
                director.getEmail(),
                director.getClassification(),
                director.getAppointmentDate(),
                director.getTermExpirationDate(),
                director.getUserId() != null);
    }
}
