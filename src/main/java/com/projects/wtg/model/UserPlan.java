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
public class UserPlan { // Renomeado de User_Plan
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
    private Plan plan; // Renomeado de plain

    private LocalDateTime started_at;
    private LocalDateTime finish_at;
}