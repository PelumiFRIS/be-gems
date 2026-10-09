package com.fris.begems.skill.dto;

import com.fris.begems.director.Director;
import com.fris.begems.director.DirectorClassification;
import java.util.UUID;

public record MatrixDirector(UUID id, String name, DirectorClassification classification) {

    public static MatrixDirector from(Director director) {
        return new MatrixDirector(director.getId(), director.getName(), director.getClassification());
    }
}
