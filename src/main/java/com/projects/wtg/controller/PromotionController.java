package com.projects.wtg.controller;

import com.projects.wtg.dto.CreatePromotionRequestDto;
import com.projects.wtg.dto.PromotionEditDto;
import com.projects.wtg.dto.PromotionEditResponseDto;
import com.projects.wtg.dto.UserDto;
import com.projects.wtg.model.User;
import com.projects.wtg.service.PromotionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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

    @PostMapping
    public ResponseEntity<UserDto> createPromotion(
            @Valid @RequestBody CreatePromotionRequestDto promotionRequestDto,
            Authentication authentication) {

        String userEmail = authentication.getName();
        User updatedUser = promotionService.createPromotion(promotionRequestDto, userEmail);

        return new ResponseEntity<>(new UserDto(updatedUser), HttpStatus.CREATED);
    }

    // Usamos PUT para atualizações, pois o cliente envia o estado desejado do recurso
    @PutMapping("/{id}/edit")
    public ResponseEntity<PromotionEditResponseDto> editPromotion(
            @PathVariable Long id,
            @Valid @RequestBody PromotionEditDto promotionDto,
            Authentication authentication) {

        String userEmail = authentication.getName();
        PromotionEditResponseDto response = promotionService.editPromotion(id, promotionDto, userEmail);

        return ResponseEntity.ok(response);
    }
}