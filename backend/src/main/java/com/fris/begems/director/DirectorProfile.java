package com.fris.begems.director;

import java.time.LocalDate;

/** The editable biodata shared by the create and update requests. */
public interface DirectorProfile {

    String name();

    String email();

    DirectorClassification classification();

    LocalDate appointmentDate();

    LocalDate termExpirationDate();

    LocalDate reElectionDate();

    LocalDate dateOfBirth();

    String phone();

    String address();

    String profession();

    String qualification();

    String experience();
}
