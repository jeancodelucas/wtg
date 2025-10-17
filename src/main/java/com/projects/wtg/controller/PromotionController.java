package com.projects.wtg.controller;

import com.projects.wtg.dto.*;
import com.projects.wtg.model.Promotion;
import com.projects.wtg.model.PromotionType;
import com.projects.wtg.service.PromotionService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/promotions")
public class PromotionController {

    private final PromotionService promotionService;

    public PromotionController(PromotionService promotionService) {
        this.promotionService = promotionService;
    }

    // --- ENDPOINT CORRIGIDO ---
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<PromotionDto> createPromotion(
            @Valid @RequestBody CreatePromotionRequestDto promotionRequestDto,
            Authentication authentication) {

        String userEmail = authentication.getName();
        Promotion createdPromotion = promotionService.createPromotion(promotionRequestDto, userEmail);
        return new ResponseEntity<>(new PromotionDto(createdPromotion), HttpStatus.CREATED);
    }

    // --- NOVO ENDPOINT PARA FINALIZAR O CADASTRO ---
    @PatchMapping("/{id}/complete")
    public ResponseEntity<PromotionDto> completePromotion(
            @PathVariable Long id,
            Authentication authentication) {

        String userEmail = authentication.getName();
        Promotion completedPromotion = promotionService.completePromotion(id, userEmail);
        return ResponseEntity.ok(new PromotionDto(completedPromotion));
    }

    @PutMapping("/{id}/edit")
    public ResponseEntity<PromotionEditResponseDto> editPromotion(
            @PathVariable Long id,
            @Valid @RequestBody PromotionEditDto promotionDto,
            Authentication authentication) {

        String userEmail = authentication.getName();
        PromotionEditResponseDto response = promotionService.editPromotion(id, promotionDto, userEmail);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/filter")
    public ResponseEntity<List<PromotionDto>> getPromotionsByFilter(
            @RequestParam(required = false) PromotionType promotionType,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude,
            @RequestParam(required = false) Double radius) {

        List<Promotion> promotions = promotionService.findWithFilters(promotionType, latitude, longitude, radius);

        List<PromotionDto> dtos = promotions.stream()
                .map(PromotionDto::new)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    @PostMapping(value = "/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadPromotionImages(
            @PathVariable Long id,
            @RequestParam("images") List<MultipartFile> files,
            Authentication authentication) {

        if (files.isEmpty() || files.size() > 6) {
            return ResponseEntity.badRequest().body("Você deve enviar de 1 a 6 imagens.");
        }

        try {
            String userEmail = authentication.getName();
            List<PromotionImageDto> uploadedImages = promotionService.uploadImages(id, files, userEmail);
            return new ResponseEntity<>(uploadedImages, HttpStatus.CREATED);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erro durante o upload do arquivo.");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/{id}/image-urls")
    public ResponseEntity<List<String>> getPromotionImageViewUrls(@PathVariable Long id) {
        List<String> urls = promotionService.getImageViewUrls(id);
        return ResponseEntity.ok(urls);
    }

    @DeleteMapping("/images/{imageId}")
    public ResponseEntity<Void> deletePromotionImage(
            @PathVariable Long imageId,
            Authentication authentication) {

        try {
            String userEmail = authentication.getName();
            promotionService.deleteImage(imageId, userEmail);
            return ResponseEntity.noContent().build();
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }
}