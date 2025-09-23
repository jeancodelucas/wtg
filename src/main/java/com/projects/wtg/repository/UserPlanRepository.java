package com.projects.wtg.repository;

import com.projects.wtg.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserPlanRepository extends JpaRepository<UserPlan, UserPlanId> {

    Optional<UserPlan> findTopByUser(User user);

    @Query("SELECT up FROM UserPlan up " +
            "JOIN FETCH up.plan p " +
            "JOIN FETCH up.user u " +
            "LEFT JOIN FETCH u.promotions " +
            "WHERE p.type = :planType AND up.planStatus = :planStatus")
    List<UserPlan> findAllByPlanTypeAndPlanStatus(PlanType planType, PlanStatus planStatus);

    @Query("SELECT up FROM UserPlan up JOIN FETCH up.plan WHERE up.user = :user AND up.planStatus = :status")
    Optional<UserPlan> findActivePlanByUser(@Param("user") User user, @Param("status") PlanStatus status);
}