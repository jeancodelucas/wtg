package com.projects.wtg.model;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class UserPlanId implements Serializable {

    private Long userId;
    private Long planId;

    public UserPlanId() {}

    public UserPlanId(Long userId, Long planId) {
        this.userId = userId;
        this.planId = planId;
    }

    // getters e setters

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserPlanId that = (UserPlanId) o;
        return Objects.equals(userId, that.userId) &&
                Objects.equals(planId, that.planId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, planId);
    }
}