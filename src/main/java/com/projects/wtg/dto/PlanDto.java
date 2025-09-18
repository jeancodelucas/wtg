package com.projects.wtg.dto;

import com.projects.wtg.model.PlanStatus;
import com.projects.wtg.model.PlanType;
import com.projects.wtg.model.UserPlan;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PlanDto {
    private String planName;
    private PlanType type;
    private BigDecimal value;
    private PlanStatus status;
    private LocalDateTime startedAt;
    private LocalDateTime finishAt;

    // Construtor que converte a entidade de relação UserPlan para este DTO
    public PlanDto(UserPlan userPlan) {
        this.planName = userPlan.getPlan().getPlanName();
        this.type = userPlan.getPlan().getType();
        this.value = userPlan.getPlan().getValue();
        this.status = userPlan.getPlanStatus();
        this.startedAt = userPlan.getStartedAt();
        this.finishAt = userPlan.getFinishAt();
    }
}