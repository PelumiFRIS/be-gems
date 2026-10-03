package com.fris.begems.director;

import com.fris.begems.director.dto.DirectorCvSummary;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DirectorCvRepository extends JpaRepository<DirectorCv, UUID> {

    @Query("select new com.fris.begems.director.dto.DirectorCvSummary(c.fileName, c.contentType, c.fileSize, "
            + "c.uploadedAt) from DirectorCv c where c.directorId = :directorId")
    Optional<DirectorCvSummary> findSummaryByDirectorId(@Param("directorId") UUID directorId);
}
