package com.projects.wtg.controller;

import com.projects.wtg.dto.*;
import com.projects.wtg.model.Promotion;
import com.projects.wtg.model.User;
import com.projects.wtg.service.PromotionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.stream.Collectors;
import java.util.List;

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

    @GetMapping("/nearby")
    public ResponseEntity<List<PromotionDto>> getNearbyPromotions(
            @RequestParam double latitude,
            @RequestParam double longitude,
            @RequestParam double radius) {

        List<Promotion> promotions = promotionService.findNearby(latitude, longitude, radius);

        List<PromotionDto> dtos = promotions.stream()
                .map(PromotionDto::new)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }
}