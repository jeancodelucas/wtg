package com.projects.wtg.repository;

import com.projects.wtg.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserPlanRepository extends JpaRepository<UserPlan, UserPlanId> {

    // --- CORREÇÃO: Alterado 'ACTIVE' para 'active' e 'PAUSED' para 'paused' ---
    @Query("SELECT up FROM UserPlan up WHERE up.user = :user AND (up.planStatus = 'active' OR (up.planStatus = 'paused' AND up.startedAt >= :now))")
    Optional<UserPlan> findActiveOrFuturePausedPlan(@Param("user") User user, @Param("now") LocalDateTime now);

    // --- NOVO MÉTODO ADICIONADO ---
    // Procura um plano para um usuário específico com um status específico.
    Optional<UserPlan> findByUserAndPlanStatus(User user, PlanStatus planStatus);

    // Métodos antigos mantidos para outras partes do sistema
    Optional<UserPlan> findTopByUserOrderByCreatedAtDesc(User user);

    @Query("SELECT up FROM UserPlan up JOIN FETCH up.plan p JOIN FETCH up.user u LEFT JOIN FETCH u.promotions WHERE p.type = :planType AND up.planStatus = :planStatus")
    List<UserPlan> findAllByPlanTypeAndPlanStatus(PlanType planType, PlanStatus planStatus);

    @Query("SELECT up FROM UserPlan up JOIN FETCH up.plan WHERE up.user = :user AND up.planStatus = :status")
    Optional<UserPlan> findActivePlanByUser(@Param("user") User user, @Param("status") PlanStatus status);

    // --- NOVOS MÉTODOS PARA O SCHEDULER ---

    // Busca todos os planos que estão ativos mas cuja data de término já passou.
    @Query("SELECT up FROM UserPlan up JOIN FETCH up.user u LEFT JOIN FETCH u.promotions WHERE up.planStatus = com.projects.wtg.model.PlanStatus.ACTIVE AND up.finishAt <= :now")
    List<UserPlan> findExpiredActivePlans(@Param("now") LocalDateTime now);

    // Busca todos os planos que estão prontos para ativar e cuja data de início já chegou.
    @Query("SELECT up FROM UserPlan up JOIN FETCH up.user u WHERE up.planStatus = com.projects.wtg.model.PlanStatus.READYTOACTIVE AND up.startedAt <= :now")
    List<UserPlan> findReadyToActivatePlans(@Param("now") LocalDateTime now);
}