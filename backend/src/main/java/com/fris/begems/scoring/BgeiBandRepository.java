package com.fris.begems.scoring;

import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BgeiBandRepository extends JpaRepository<BgeiBand, BigDecimal> {

    Optional<BgeiBand> findFirstByMinPctLessThanEqualAndMaxPctGreaterThanEqual(BigDecimal pct, BigDecimal samePct);
}
