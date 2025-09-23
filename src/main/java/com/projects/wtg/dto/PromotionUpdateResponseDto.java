package com.projects.wtg.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO para a resposta customizada quando a reativação da promoção é negada
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL) // Não inclui campos nulos no JSON final
public class PromotionUpdateResponseDto {
    private PromotionDto promotion;
    private String message;
    private PlanDto userPlan;
}