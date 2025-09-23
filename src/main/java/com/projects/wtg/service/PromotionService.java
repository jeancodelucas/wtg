package com.projects.wtg.service;

import com.projects.wtg.dto.PlanDto;
import com.projects.wtg.dto.PromotionDto;
import com.projects.wtg.dto.PromotionEditDto;
import com.projects.wtg.dto.PromotionEditResponseDto;
import com.projects.wtg.model.*;
import com.projects.wtg.repository.AccountRepository;
import com.projects.wtg.repository.PromotionRepository;
import com.projects.wtg.repository.UserPlanRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class PromotionService {

    private final PromotionRepository promotionRepository;
    private final AccountRepository accountRepository;
    private final UserPlanRepository userPlanRepository;

    public PromotionService(PromotionRepository promotionRepository, AccountRepository accountRepository, UserPlanRepository userPlanRepository) {
        this.promotionRepository = promotionRepository;
        this.accountRepository = accountRepository;
        this.userPlanRepository = userPlanRepository;
    }

    @Transactional
    public PromotionEditResponseDto editPromotion(Long promotionId, PromotionEditDto dto, String userEmail) {
        Account account = accountRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado."));
        User user = account.getUser();

        Promotion promotion = promotionRepository.findByIdAndUser(promotionId, user)
                .orElseThrow(() -> new EntityNotFoundException("Promoção com ID " + promotionId + " não encontrada ou não pertence a este usuário."));

        UserPlan userPlan = userPlanRepository.findActivePlanByUser(user)
                .orElseThrow(() -> new IllegalStateException("Usuário não possui plano ativo."));

        LocalDateTime planStartedAt = userPlan.getStartedAt();

        // Atualiza os campos básicos
        updatePromotionFields(promotion, dto);

        if (Boolean.TRUE.equals(promotion.getAllowUserActivePromotion())) {
            promotion.setActive(dto.getActive());
            Promotion updatedPromotion = promotionRepository.save(promotion);
            return PromotionEditResponseDto.builder()
                    .promotion(new PromotionDto(updatedPromotion))
                    .message("Promoção atualizada com sucesso.")
                    .build();
        }

        if (Boolean.TRUE.equals(dto.getActive())) { // Tentando ativar sem permissão
            String message = "Promoção atualizada, mas não pôde ser reativada.";
            if (userPlan.getPlan().getType() == PlanType.FREE) {
                LocalDateTime nextActivationDate = planStartedAt.plusDays(7);
                message = "A próxima ativação só pode ser feita após " + nextActivationDate.toLocalDate();
            }
            return PromotionEditResponseDto.builder()
                    .promotion(new PromotionDto(promotion))
                    .message(message)
                    .userPlan(new PlanDto(userPlan))
                    .build();
        }

        // Se a permissão for false e o usuário estiver desativando (ou mantendo desativado)
        promotion.setActive(false);
        Promotion updatedPromotion = promotionRepository.save(promotion);
        return PromotionEditResponseDto.builder()
                .promotion(new PromotionDto(updatedPromotion))
                .message("Promoção desativada.")
                .build();
    }

    private void updatePromotionFields(Promotion promotion, PromotionEditDto dto) {
        promotion.setTitle(dto.getTitle());
        promotion.setDescription(dto.getDescription());
        promotion.setFree(dto.isFree());
        promotion.setObs(dto.getObs());
    }
}