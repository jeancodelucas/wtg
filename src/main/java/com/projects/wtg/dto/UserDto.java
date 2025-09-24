package com.projects.wtg.dto;

import com.projects.wtg.model.PlanStatus;
import com.projects.wtg.model.User;
import com.projects.wtg.model.UserType;
import lombok.Data;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Data
public class UserDto {
    private Long id;
    private String firstName;
    private String fullName;
    private String phone;
    private String pictureUrl;
    private String email;
    private Boolean active;
    private PlanDto activePlan;
    private UserType userType;
    private List<PromotionDto> promotions;

    public UserDto(User user) {
        this.id = user.getId();
        this.firstName = user.getFirstName();
        this.fullName = user.getFullName();
        this.phone = user.getPhone();
        this.pictureUrl = user.getPictureUrl();
        this.userType = user.getUserType();

        if (user.getAccount() != null) {
            this.email = user.getAccount().getEmail();
            this.active = user.getAccount().getActive();
        }

        // CORREÇÃO: Lógica ajustada e bloco duplicado removido.
        // Agora, ele pega o primeiro plano encontrado, independentemente do status.
        if (user.getUserPlans() != null && !user.getUserPlans().isEmpty()) {
            this.activePlan = user.getUserPlans().stream()
                    .findFirst()
                    .map(PlanDto::new)
                    .orElse(null);
        }

        if (user.getPromotions() != null) {
            this.promotions = user.getPromotions().stream()
                    .map(PromotionDto::new)
                    .collect(Collectors.toList());
        }
    }
}