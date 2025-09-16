package com.projects.wtg.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name= "user_plan", schema = "appwtg")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User_Plan {
    @EmbeddedId
    private UserPlanId id;

    @ManyToOne
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @MapsId("planId")
    @JoinColumn(name = "plan_id")
    private Plan plain;

    private LocalDateTime started_at;
    private LocalDateTime finish_at;


}
