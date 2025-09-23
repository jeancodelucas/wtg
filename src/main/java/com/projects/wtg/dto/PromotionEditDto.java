package com.projects.wtg.dto;

import lombok.Data;

// DTO para receber os dados de atualização de uma promoção
@Data
public class PromotionEditDto {
    private String title;
    private String description;
    private boolean free;
    private String obs;
    private Boolean active;
    private Long planId;
    // O campo allowUserActivePromotion não é incluído, pois é controlado pelo sistema
}