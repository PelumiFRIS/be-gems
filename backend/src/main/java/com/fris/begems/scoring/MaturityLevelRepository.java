package com.fris.begems.scoring;

import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MaturityLevelRepository extends JpaRepository<MaturityLevel, Integer> {

    Optional<MaturityLevel> findFirstByMinScoreLessThanEqualAndMaxScoreGreaterThanEqual(BigDecimal score,
            BigDecimal sameScore);
}
