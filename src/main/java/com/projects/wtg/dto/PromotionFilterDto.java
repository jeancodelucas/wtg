package com.projects.wtg.dto;

import com.projects.wtg.model.PromotionType;
import lombok.Data;

@Data
public class PromotionFilterDto {
    private PromotionType promotionType;
    private Double latitude;
    private Double longitude;
    private Double radius; // em km
}