package com.projects.wtg.dto;

import jakarta.validation.Valid;
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
    @Valid // Garante que o objeto de endereço aninhado seja validado
    private AddressDto address;
    // O campo allowUserActivePromotion não é incluído, pois é controlado pelo sistema
}