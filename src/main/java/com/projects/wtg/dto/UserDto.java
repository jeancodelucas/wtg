package com.projects.wtg.dto;

import com.projects.wtg.model.PlanStatus;
import com.projects.wtg.model.User;
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
    private PlanDto activePlan;

    public UserDto(User user) {
        this.id = user.getId();
        this.firstName = user.getFirstName();
        this.fullName = user.getFullName();
        this.phone = user.getPhone();
        this.pictureUrl = user.getPictureUrl();
        if (user.getAccount() != null) {
            this.email = user.getAccount().getEmail();
        }

        // 2. Lógica para encontrar e mapear o plano ativo
        if (user.getUserPlans() != null) {
            Optional<PlanDto> activePlanDto = user.getUserPlans().stream()
                    .filter(up -> up.getStatus() == PlanStatus.ACTIVE)
                    .findFirst()
                    .map(PlanDto::new); // Converte o UserPlan encontrado para PlanDto

            this.activePlan = activePlanDto.orElse(null);
        }
    }
}