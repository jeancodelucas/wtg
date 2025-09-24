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

    @Transactional
    public User createPromotion(CreatePromotionRequestDto dto, String userEmail) {
        Account account = accountRepository.findByEmailWithUserAndPlans(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("Utilizador não encontrado."));
        User user = account.getUser();

        if (user.getPromotions() != null && !user.getPromotions().isEmpty()) {
            throw new PromotionAlreadyExistsException("Não é possível criar um novo evento, pois já existe um evento vinculado a este utilizador.");
        }

        Promotion promotion = buildPromotionFromDto(dto);

        handlePromotionActivation(user, promotion, dto.getActive(), dto.getPlanId());

        user.addPromotion(promotion);
        return userRepository.save(user);
    }

    @Transactional
    public PromotionEditResponseDto editPromotion(Long promotionId, PromotionEditDto dto, String userEmail) {
        Account account = accountRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("Utilizador não encontrado."));
        User user = account.getUser();

        Promotion promotion = promotionRepository.findByIdAndUser(promotionId, user)
                .orElseThrow(() -> new EntityNotFoundException("Promoção com ID " + promotionId + " não encontrada ou não pertence a este utilizador."));

        updatePromotionFields(promotion, dto);

        handlePromotionActivation(user, promotion, dto.getActive(), dto.getPlanId());

        Promotion updatedPromotion = promotionRepository.save(promotion);
        PlanDto updatedPlanDto = userPlanRepository.findTopByUserOrderByCreatedAtDesc(user).map(PlanDto::new).orElse(null);

        return PromotionEditResponseDto.builder()
                .promotion(new PromotionDto(updatedPromotion))
                .userPlan(updatedPlanDto)
                .message("Promoção atualizada com sucesso.")
                .build();
    }

    private void handlePromotionActivation(User user, Promotion promotion, Boolean active, Long planId) {
        // REGRA 1: Se 'active' for false, apenas desativa a promoção e termina a execução.
        if (Boolean.FALSE.equals(active)) {
            promotion.setActive(false);
            return;
        }

        // Se 'active' for nulo ou true, continua para as próximas regras.
        promotion.setActive(true);
        LocalDateTime now = LocalDateTime.now();
        Optional<UserPlan> existingPlanOpt = userPlanRepository.findActiveOrFuturePausedPlan(user, now);

        if (planId != null) {
            // REGRA 5: Se um planId foi enviado
            if (existingPlanOpt.isEmpty()) {
                // REGRA 5.1: Se não há plano válido, cria um novo com o planId fornecido.
                Plan planToAssign = planRepository.findById(planId)
                        .orElseThrow(() -> new EntityNotFoundException("Plano com ID " + planId + " não encontrado."));

                createNewUserPlan(user, planToAssign, now);
            }
            // Se já existe um plano válido, não faz nada com a tabela user_plan.
        } else {
            // REGRA 2, 3 e 4: Se um planId NÃO foi enviado
            if (existingPlanOpt.isEmpty()) {
                // REGRA 4: Se não há plano válido, cria um novo plano FREE.
                Plan freePlan = planRepository.findByType(PlanType.FREE)
                        .orElseThrow(() -> new IllegalStateException("Plano 'FREE' não encontrado no banco de dados."));

                UserPlan newUserPlan = createNewUserPlan(user, freePlan, now);
                newUserPlan.setFinishAt(now.plusHours(24)); // Regra específica do plano FREE
            }
            // REGRA 2 e 3: Se já existe um plano válido, não faz nada com a tabela user_plan.
        }
    }

    private UserPlan createNewUserPlan(User user, Plan plan, LocalDateTime now) {
        UserPlan newUserPlan = UserPlan.builder()
                .id(new UserPlanId(null, plan.getId()))
                .user(user)
                .plan(plan)
                .planStatus(PlanStatus.ACTIVE)
                .startedAt(now)
                .build();

        setFinishAtByPlanType(newUserPlan, now);
        user.getUserPlans().add(newUserPlan);
        return newUserPlan;
    }

    private void setFinishAtByPlanType(UserPlan userPlan, LocalDateTime startDate) {
        if (startDate == null) return;
        switch (userPlan.getPlan().getType()) {
            case FREE:
                userPlan.setFinishAt(startDate.plusHours(24));
                break;
            case MONTHLY:
                userPlan.setFinishAt(startDate.plusDays(30));
                break;
            case PARTNER:
                userPlan.setFinishAt(startDate.plusYears(1)); // NOVA REGRA
                break;
            default:
                break;
        }
    }

    private Promotion buildPromotionFromDto(CreatePromotionRequestDto dto) {
        Promotion promotion = new Promotion();
        promotion.setTitle(dto.getTitle());
        promotion.setDescription(dto.getDescription());
        promotion.setFree(dto.isFree());
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

    private void updatePromotionFields(Promotion promotion, PromotionEditDto dto) {
        promotion.setTitle(dto.getTitle());
        promotion.setDescription(dto.getDescription());
        promotion.setFree(dto.isFree());
        promotion.setObs(dto.getObs());
    }
}