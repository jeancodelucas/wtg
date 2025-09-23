package com.projects.wtg.service;

import com.projects.wtg.dto.*;
import com.projects.wtg.exception.PromotionAlreadyExistsException;
import com.projects.wtg.model.*;
import com.projects.wtg.repository.AccountRepository;
import com.projects.wtg.repository.PromotionRepository;
import com.projects.wtg.repository.UserPlanRepository;
import com.projects.wtg.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class PromotionService {

    private final PromotionRepository promotionRepository;
    private final AccountRepository accountRepository;
    private final UserPlanRepository userPlanRepository;
    private final UserRepository userRepository;

    public PromotionService(PromotionRepository promotionRepository, AccountRepository accountRepository, UserPlanRepository userPlanRepository, UserRepository userRepository) {
        this.promotionRepository = promotionRepository;
        this.accountRepository = accountRepository;
        this.userPlanRepository = userPlanRepository;
        this.userRepository = userRepository;
    }

    // --- CORREÇÃO: A assinatura do método agora usa o DTO correto ---
    @Transactional
    public User createPromotion(CreatePromotionRequestDto dto, String userEmail) {
        Account account = accountRepository.findByEmailWithUserAndPlans(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado."));
        User user = account.getUser();

        if (user.getPromotions() != null && !user.getPromotions().isEmpty()) {
            throw new PromotionAlreadyExistsException("Não é possível criar um novo evento, pois já existe um evento vinculado a este usuário.");
        }

        Promotion promotion = new Promotion();
        promotion.setTitle(dto.getTitle());
        promotion.setDescription(dto.getDescription());
        promotion.setActive(dto.getActive());
        promotion.setFree(dto.isFree());
        promotion.setObs(dto.getObs());
        promotion.setAllowUserActivePromotion(true);

        Address address = new Address();
        if (dto.getAddress() != null) {
            address.setAddress(dto.getAddress().getAddress());
            address.setNumber(dto.getAddress().getNumber());
            address.setComplement(dto.getAddress().getComplement());
            address.setReference(dto.getAddress().getReference());
            address.setPostalCode(dto.getAddress().getPostalCode());
            address.setObs(dto.getAddress().getObs());
        }

        promotion.setAddress(address);

        if (Boolean.TRUE.equals(dto.getActive())) {
            UserPlan userPlan = userPlanRepository.findActivePlanByUser(user)
                    .orElseThrow(() -> new IllegalStateException("Usuário não possui plano ativo."));

            LocalDateTime now = LocalDateTime.now();

            switch (userPlan.getPlan().getType()) {
                case FREE:
                    userPlan.setStartedAt(now);
                    userPlan.setFinishAt(now.plusHours(24));
                    break;
                case MONTHLY:
                    userPlan.setFinishAt(now.plusMonths(1));
                    break;
                default:
                    break;
            }
            userPlanRepository.save(userPlan);
        }

        user.addPromotion(promotion);
        return userRepository.save(user);
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

        updatePromotionFields(promotion, dto);

        if (Boolean.TRUE.equals(promotion.getAllowUserActivePromotion())) {
            promotion.setActive(dto.getActive());
            Promotion updatedPromotion = promotionRepository.save(promotion);
            return PromotionEditResponseDto.builder()
                    .promotion(new PromotionDto(updatedPromotion))
                    .message("Promoção atualizada com sucesso.")
                    .build();
        }

        if (Boolean.TRUE.equals(dto.getActive())) {
            String message = "Promoção atualizada, mas não pôde ser reativada.";
            if (userPlan.getPlan().getType() == PlanType.FREE && userPlan.getFinishAt() != null) {
                LocalDateTime nextActivationDate = userPlan.getFinishAt().plusDays(7);
                message = "A próxima ativação só pode ser feita após " + nextActivationDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm"));
            }
            return PromotionEditResponseDto.builder()
                    .promotion(new PromotionDto(promotion))
                    .message(message)
                    .userPlan(new PlanDto(userPlan))
                    .build();
        } else {
            promotion.setActive(false);
            Promotion updatedPromotion = promotionRepository.save(promotion);
            return PromotionEditResponseDto.builder()
                    .promotion(new PromotionDto(updatedPromotion))
                    .message("Promoção desativada.")
                    .build();
        }
    }

    private void updatePromotionFields(Promotion promotion, PromotionEditDto dto) {
        promotion.setTitle(dto.getTitle());
        promotion.setDescription(dto.getDescription());
        promotion.setFree(dto.isFree());
        promotion.setObs(dto.getObs());
    }
}