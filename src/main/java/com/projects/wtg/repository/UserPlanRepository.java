package com.projects.wtg.repository;

import com.projects.wtg.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserPlanRepository extends JpaRepository<UserPlan, UserPlanId> {

    @Query("SELECT up FROM UserPlan up " +
            "JOIN FETCH up.plan p " +
            "JOIN FETCH up.user u " +
            "LEFT JOIN FETCH u.promotions " +
            "WHERE p.type = :planType AND up.planStatus = :planStatus")
    List<UserPlan> findAllByPlanTypeAndPlanStatus(PlanType planType, PlanStatus planStatus);

    // buscar o plano ativo de um usuário específico
    @Query("SELECT up FROM UserPlan up JOIN FETCH up.plan WHERE up.user = :user AND up.planStatus = 'ACTIVE'")
    Optional<UserPlan> findActivePlanByUser(@Param("user") User user);
}