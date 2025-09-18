package com.projects.wtg.repository;

import com.projects.wtg.model.Plan;
import com.projects.wtg.model.PlanType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlanRepository extends JpaRepository<Plan, Long> {
    Optional<Plan> findByType(PlanType type);
}