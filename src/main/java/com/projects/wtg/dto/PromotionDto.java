package com.projects.wtg.dto;

import com.projects.wtg.model.Promotion;
import com.projects.wtg.model.PromotionType;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

// DTO para receber os dados de atualização de uma promoção
@Data
@NoArgsConstructor
public class PromotionDto {
    private Long id;
    private String title;
    private String description;
    private boolean free;
    private String obs;
    private Boolean active;
    private AddressDto address;
    private PromotionType promotionType;
    private boolean highlight;
    private List<PromotionImageDto> images;

    // Construtor para facilitar a conversão da entidade para DTO
    public PromotionDto(Promotion promotion) {
        this.title = promotion.getTitle();
        this.description = promotion.getDescription();
        this.free = promotion.isFree();
        this.obs = promotion.getObs();
        this.active = promotion.getActive();
        this.promotionType = promotion.getPromotionType();
        this.highlight = promotion.isHighlight();
        this.highlight = promotion.isHighlight();

        if (promotion.getAddress() != null) {
            this.address = new AddressDto(promotion.getAddress());
        }
        if (promotion.getImages() != null) {
            this.images = promotion.getImages().stream()
                    .map(PromotionImageDto::new)
                    .collect(Collectors.toList());
        }
    }
}