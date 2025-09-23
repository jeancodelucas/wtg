package com.projects.wtg.service;

import com.projects.wtg.dto.PromotionDto;
import com.projects.wtg.dto.PromotionUpdateResponseDto;
import com.projects.wtg.model.*;
import com.projects.wtg.repository.AccountRepository;
import com.projects.wtg.repository.PromotionRepository;
import com.projects.wtg.repository.UserPlanRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

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
    public PromotionUpdateResponseDto updatePromotion(Long promotionId, PromotionDto promotionDto, String userEmail) {
        // Busca o usuário logado
        Account account = accountRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado."));
        User user = account.getUser();

        // Busca a promoção específica do usuário para garantir que ele só edite o que é dele
        Promotion promotion = promotionRepository.findByIdAndUser(promotionId, user)
                .orElseThrow(() -> new EntityNotFoundException("Promoção com ID " + promotionId + " não encontrada ou não pertence a este usuário."));

        // 1. Captura o UserPlan do usuário
        UserPlan userPlan = userPlanRepository.findActivePlanByUser(user)
                .orElseThrow(() -> new IllegalStateException("Usuário não possui plano ativo."));

        LocalDateTime planStartedAt = userPlan.getStartedAt();

        // 2. Verifica a permissão para ativar a promoção
        if (Boolean.TRUE.equals(promotion.getAllowUserActivePromotion())) {
            // Se a permissão for true, atualiza todos os campos, incluindo o 'active'
            updatePromotionFields(promotion, promotionDto);
            promotion.setActive(promotionDto.getActive());

            Promotion updatedPromotion = promotionRepository.save(promotion);
            return new PromotionUpdateResponseDto(new PromotionDto(updatedPromotion), "Promoção atualizada com sucesso.", null);

        } else if (Boolean.FALSE.equals(promotionDto.getActive())) {
            // Se a permissão for false, mas o usuário estiver tentando desativar, permita.
            updatePromotionFields(promotion, promotionDto);
            promotion.setActive(false);

            Promotion updatedPromotion = promotionRepository.save(promotion);
            return new PromotionUpdateResponseDto(new PromotionDto(updatedPromotion), "Promoção desativada.", null);

        } else {
            // Se a permissão for false e o usuário estiver tentando ATIVAR
            updatePromotionFields(promotion, promotionDto);
            // O campo 'active' NÃO é atualizado

            Promotion updatedPromotion = promotionRepository.save(promotion);
            String message = "Promoção atualizada, mas não pôde ser reativada.";

            if (userPlan.getPlan().getType() == PlanType.FREE) {
                LocalDateTime nextActivationDate = planStartedAt.plus(7, ChronoUnit.DAYS);
                message = "A próxima ativação só pode ser feita após " + nextActivationDate.toLocalDate();
            }

            return new PromotionUpdateResponseDto(new PromotionDto(updatedPromotion), message, null);
        }
    }

    private void updatePromotionFields(Promotion promotion, PromotionDto dto) {
        promotion.setTitle(dto.getTitle());
        promotion.setDescription(dto.getDescription());
        promotion.setFree(dto.isFree());
        promotion.setObs(dto.getObs());
    }
}