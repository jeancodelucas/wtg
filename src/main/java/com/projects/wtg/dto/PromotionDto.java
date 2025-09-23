package com.projects.wtg.dto;

import com.projects.wtg.model.Promotion;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO para receber os dados de atualização de uma promoção
@Data
@NoArgsConstructor
public class PromotionDto {
    private String title;
    private String description;
    private boolean free;
    private String obs;
    private Boolean active;
    private AddressDto address;

    // Construtor para facilitar a conversão da entidade para DTO
    public PromotionDto(Promotion promotion) {
        this.title = promotion.getTitle();
        this.description = promotion.getDescription();
        this.free = promotion.isFree();
        this.obs = promotion.getObs();
        this.active = promotion.getActive();
        if (promotion.getAddress() != null) {
            this.address = new AddressDto(promotion.getAddress());
        }
    }
}