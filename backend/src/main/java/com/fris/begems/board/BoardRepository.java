package com.fris.begems.board;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BoardRepository extends JpaRepository<Board, UUID> {

    List<Board> findByOrganizationId(UUID organizationId);

    Optional<Board> findByIdAndOrganizationId(UUID id, UUID organizationId);

    /** Serialises roster changes on one board so concurrent duplicate submissions can't both pass a check. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Board b where b.id = :id and b.organizationId = :organizationId")
    Optional<Board> lockByIdAndOrganizationId(@Param("id") UUID id, @Param("organizationId") UUID organizationId);
}
