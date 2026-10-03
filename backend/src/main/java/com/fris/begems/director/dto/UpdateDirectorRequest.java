package com.fris.begems.director.dto;

import com.fris.begems.director.DirectorClassification;
import com.fris.begems.director.DirectorProfile;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record UpdateDirectorRequest(
        @NotBlank @Size(max = 255) String name,
        @NotBlank @Email @Size(max = 255) String email,
        @NotNull DirectorClassification classification,
        LocalDate appointmentDate,
        LocalDate termExpirationDate,
        LocalDate reElectionDate,
        @Past LocalDate dateOfBirth,
        @Size(max = 50) String phone,
        String address,
        @Size(max = 255) String profession,
        String qualification,
        String experience) implements DirectorProfile {
}
