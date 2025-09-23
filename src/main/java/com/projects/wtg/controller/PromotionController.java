package com.projects.wtg.controller;

import com.projects.wtg.dto.PromotionDto;
import com.projects.wtg.dto.PromotionUpdateResponseDto;
import com.projects.wtg.service.PromotionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/promotions")
public class PromotionController {

    private final PromotionService promotionService;

    public PromotionController(PromotionService promotionService) {
        this.promotionService = promotionService;
    }

    // Usamos PUT para atualizações completas da entidade e o ID na URL, que é mais RESTful
    @PutMapping("/{id}/edit")
    public ResponseEntity<PromotionUpdateResponseDto> editPromotion(
            @PathVariable Long id,
            @Valid @RequestBody PromotionDto promotionDto,
            Authentication authentication) {

        String userEmail = authentication.getName();
        PromotionUpdateResponseDto response = promotionService.updatePromotion(id, promotionDto, userEmail);

        return ResponseEntity.ok(response);
    }
}