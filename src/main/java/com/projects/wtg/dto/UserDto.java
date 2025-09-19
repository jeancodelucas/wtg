package com.projects.wtg.dto;

import com.projects.wtg.model.PlanStatus;
import com.projects.wtg.model.User;
import com.projects.wtg.model.UserType;
import lombok.Data;

import java.util.Optional;

@Data
public class UserDto {
    private Long id;
    private String firstName;
    private String fullName;
    private String phone;
    private String pictureUrl;
    private String email;
    private Boolean active; // <-- CAMPO QUE ESTAVA FALTANDO
    private PlanDto activePlan;
    private UserType userType;

    public UserDto(User user) {
        this.id = user.getId();
        this.firstName = user.getFirstName();
        this.fullName = user.getFullName();
        this.phone = user.getPhone();
        this.pictureUrl = user.getPictureUrl();
        this.userType = user.getUserType();

        if (user.getAccount() != null) {
            this.email = user.getAccount().getEmail();
            this.active = user.getAccount().getActive(); // Agora esta linha funciona
        }

        if (user.getUserPlans() != null) {
            Optional<PlanDto> activePlanDto = user.getUserPlans().stream()
                    .filter(up -> up.getPlanStatus() == PlanStatus.ACTIVE)
                    .findFirst()
                    .map(PlanDto::new);

            this.activePlan = activePlanDto.orElse(null);
        }
    }
}