package com.projects.wtg.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name= "user_plan", schema = "appwtg")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPlan {
    @EmbeddedId
    private UserPlanId id;

    @JsonBackReference
    @ToString.Exclude
    @ManyToOne
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @ToString.Exclude
    @MapsId("planId")
    @JoinColumn(name = "plan_id")
    private Plan plan;

    private LocalDateTime startedAt;
    private LocalDateTime finishAt;

    // Mapeia explicitamente o atributo 'planStatus' para a coluna 'status' do banco de dados.
    @Column(name = "status")
    private PlanStatus planStatus;

    private Boolean paymentMade;
}