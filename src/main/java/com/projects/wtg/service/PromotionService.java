package com.projects.wtg.service;

import com.projects.wtg.dto.*;
import com.projects.wtg.exception.PromotionAlreadyExistsException;
import com.projects.wtg.model.*;
import com.projects.wtg.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
public class PromotionService {

    private final PromotionRepository promotionRepository;
    private final AccountRepository accountRepository;
    private final UserPlanRepository userPlanRepository;
    private final UserRepository userRepository;
    private final PlanRepository planRepository;

    public PromotionService(PromotionRepository promotionRepository, AccountRepository accountRepository, UserPlanRepository userPlanRepository, UserRepository userRepository, PlanRepository planRepository) {
        this.promotionRepository = promotionRepository;
        this.accountRepository = accountRepository;
        this.userPlanRepository = userPlanRepository;
        this.userRepository = userRepository;
        this.planRepository = planRepository;
    }

    // createPromotion e métodos auxiliares permanecem os mesmos...
    @Transactional
    public User createPromotion(CreatePromotionRequestDto dto, String userEmail) {
        Account account = accountRepository.findByEmailWithUserAndPlans(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado."));
        User user = account.getUser();

        if (user.getPromotions() != null && !user.getPromotions().isEmpty()) {
            throw new PromotionAlreadyExistsException("Não é possível criar um novo evento, pois já existe um evento vinculado a este usuário.");
        }

        Promotion promotion = buildPromotionFromDto(dto);
        user.addPromotion(promotion);

        if (Boolean.TRUE.equals(dto.getActive())) {
            handleUserPlanActivation(user);
        }

        return userRepository.save(user);
    }

    private void handleUserPlanActivation(User user) {
        Optional<UserPlan> activeUserPlanOpt = userPlanRepository.findActivePlanByUser(user, PlanStatus.ACTIVE);
        LocalDateTime now = LocalDateTime.now();

        if (activeUserPlanOpt.isEmpty()) {
            Plan freePlan = planRepository.findByType(PlanType.FREE)
                    .orElseThrow(() -> new IllegalStateException("Plano 'FREE' não encontrado no banco de dados."));

            UserPlan newUserPlan = UserPlan.builder()
                    .id(new UserPlanId(null, freePlan.getId()))
                    .user(user)
                    .plan(freePlan)
                    .planStatus(PlanStatus.ACTIVE)
                    .paymentMade(true)
                    .startedAt(now)
                    .finishAt(now.plusHours(24))
                    .build();
            user.getUserPlans().add(newUserPlan);
        } else {
            UserPlan activeUserPlan = activeUserPlanOpt.get();
            if (activeUserPlan.getStartedAt() == null && activeUserPlan.getFinishAt() == null) {
                activeUserPlan.setStartedAt(now);
                setFinishAtByPlanType(activeUserPlan, now);
            } else if (activeUserPlan.getStartedAt() != null && activeUserPlan.getFinishAt() == null) {
                setFinishAtByPlanType(activeUserPlan, now);
            }
            userPlanRepository.save(activeUserPlan);
        }
    }

    private void setFinishAtByPlanType(UserPlan userPlan, LocalDateTime now) {
        switch (userPlan.getPlan().getType()) {
            case FREE:
                userPlan.setFinishAt(now.plusHours(24));
                break;
            case MONTHLY:
                userPlan.setFinishAt(now.plusDays(30));
                break;
            case PARTNER:
                break;
            default:
                break;
        }
    }

    private Promotion buildPromotionFromDto(CreatePromotionRequestDto dto) {
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
        return promotion;
    }

    // --- MÉTODO EDITPROMOTION TOTALMENTE REESCRITO ---
    @Transactional
    public PromotionEditResponseDto editPromotion(Long promotionId, PromotionEditDto dto, String userEmail) {
        Account account = accountRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado."));
        User user = account.getUser();

        Promotion promotion = promotionRepository.findByIdAndUser(promotionId, user)
                .orElseThrow(() -> new EntityNotFoundException("Promoção com ID " + promotionId + " não encontrada ou não pertence a este usuário."));

        // Regra 1: Atualizar o status da promoção
        if (Boolean.TRUE.equals(promotion.getAllowUserActivePromotion())) {
            promotion.setActive(dto.getActive());
        }

        // Regra 2: Lógica de criação do UserPlan se a promoção for ativada
        if (Boolean.TRUE.equals(dto.getActive())) {
            Optional<UserPlan> userPlanOpt = userPlanRepository.findTopByUser(user);

            if (userPlanOpt.isEmpty()) {
                LocalDateTime now = LocalDateTime.now();
                Plan planToAssign;

                if (dto.getPlanId() != null) {
                    // Se um planId foi enviado, usa esse plano
                    planToAssign = planRepository.findById(dto.getPlanId())
                            .orElseThrow(() -> new EntityNotFoundException("Plano com ID " + dto.getPlanId() + " não encontrado."));
                } else {
                    // Se não, usa o plano FREE como padrão
                    planToAssign = planRepository.findByType(PlanType.FREE)
                            .orElseThrow(() -> new IllegalStateException("Plano 'FREE' não encontrado no banco de dados."));
                }

                UserPlan newUserPlan = UserPlan.builder()
                        .id(new UserPlanId(null, planToAssign.getId()))
                        .user(user)
                        .plan(planToAssign)
                        .planStatus(PlanStatus.ACTIVE)
                        .paymentMade(true) // Assumindo pagamento como true ao ativar
                        .startedAt(now)
                        .build();

                // Define finishAt com base no tipo de plano
                setFinishAtByPlanType(newUserPlan, now);

                user.getUserPlans().add(newUserPlan);
                userRepository.save(user); // Salva o usuário para cascatear a criação do UserPlan
            }
        }

        updatePromotionFields(promotion, dto);
        Promotion updatedPromotion = promotionRepository.save(promotion);

        PlanDto updatedPlanDto = userPlanRepository.findTopByUser(user)
                .map(PlanDto::new)
                .orElse(null);

        return PromotionEditResponseDto.builder()
                .promotion(new PromotionDto(updatedPromotion))
                .userPlan(updatedPlanDto)
                .message("Promoção atualizada com sucesso.")
                .build();
    }

    private void updatePromotionFields(Promotion promotion, PromotionEditDto dto) {
        promotion.setTitle(dto.getTitle());
        promotion.setDescription(dto.getDescription());
        promotion.setFree(dto.isFree());
        promotion.setObs(dto.getObs());
    }
}