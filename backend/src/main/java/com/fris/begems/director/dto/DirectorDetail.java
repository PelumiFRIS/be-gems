package com.fris.begems.director.dto;

import com.fris.begems.committee.dto.DirectorCommitteeMembership;
import com.fris.begems.director.Director;
import com.fris.begems.director.DirectorClassification;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** The full biodata view — kept off the board-wide list so personal details aren't sent to every user. */
public record DirectorDetail(
        UUID id,
        UUID boardId,
        String name,
        String email,
        DirectorClassification classification,
        LocalDate appointmentDate,
        LocalDate termExpirationDate,
        LocalDate reElectionDate,
        LocalDate dateOfBirth,
        String phone,
        String address,
        String profession,
        String qualification,
        String experience,
        boolean hasPortalAccess,
        DirectorCvSummary cv,
        List<DirectorCommitteeMembership> committees) {

    public static DirectorDetail from(Director director, DirectorCvSummary cv,
            List<DirectorCommitteeMembership> committees) {
        return new DirectorDetail(
                director.getId(),
                director.getBoardId(),
                director.getName(),
                director.getEmail(),
                director.getClassification(),
                director.getAppointmentDate(),
                director.getTermExpirationDate(),
                director.getReElectionDate(),
                director.getDateOfBirth(),
                director.getPhone(),
                director.getAddress(),
                director.getProfession(),
                director.getQualification(),
                director.getExperience(),
                director.getUserId() != null,
                cv,
                committees);
    }
}
