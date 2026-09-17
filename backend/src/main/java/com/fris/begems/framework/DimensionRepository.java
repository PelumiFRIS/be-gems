package com.fris.begems.framework;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DimensionRepository extends JpaRepository<Dimension, UUID> {

    List<Dimension> findByFrameworkIdOrderByDisplayOrder(UUID frameworkId);
}
