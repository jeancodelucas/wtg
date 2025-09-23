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

        // Se um planId for enviado, a nova lógica de transição de plano é acionada
        if (dto.getPlanId() != null) {
            handlePlanTransition(user, dto.getPlanId());
        }

        return userRepository.save(user);
    }

    // --- NOVA LÓGICA CENTRALIZADA PARA TRANSIÇÃO DE PLANOS ---
    private void handlePlanTransition(User user, Long newPlanId) {
        LocalDateTime now = LocalDateTime.now();
        Optional<UserPlan> currentUserPlanOpt = userPlanRepository.findActivePlanByUser(user, PlanStatus.ACTIVE);

        Plan newPlan = planRepository.findById(newPlanId)
                .orElseThrow(() -> new EntityNotFoundException("Plano com ID " + newPlanId + " não encontrado."));

        if (currentUserPlanOpt.isPresent()) {
            UserPlan currentUserPlan = currentUserPlanOpt.get();

            // REGRA 1 e 2: Se o plano atual expirou ou está inativo, inativa o antigo e cria um novo ativo.
            if (currentUserPlan.getPlanStatus() == PlanStatus.INACTIVE ||
                    (currentUserPlan.getFinishAt() != null && !currentUserPlan.getFinishAt().isAfter(now))) {

                currentUserPlan.setPlanStatus(PlanStatus.INACTIVE);
                userPlanRepository.save(currentUserPlan);

                createNewActiveUserPlan(user, newPlan, now);
            }
            // REGRA 3: Se o plano atual está ativo e não expirou, cria um novo plano pausado.
            else if (currentUserPlan.getPlanStatus() == PlanStatus.ACTIVE &&
                    (currentUserPlan.getFinishAt() == null || currentUserPlan.getFinishAt().isAfter(now))) {

                UserPlan pausedUserPlan = UserPlan.builder()
                        .id(new UserPlanId(null, newPlan.getId()))
                        .user(user)
                        .plan(newPlan)
                        .planStatus(PlanStatus.PAUSED)
                        .startedAt(currentUserPlan.getFinishAt()) // Inicia quando o outro terminar
                        .build();
                setFinishAtByPlanType(pausedUserPlan, pausedUserPlan.getStartedAt());
                user.getUserPlans().add(pausedUserPlan);
            }
        } else {
            // Se não há plano ativo, simplesmente cria um novo.
            createNewActiveUserPlan(user, newPlan, now);
        }
    }

    private void createNewActiveUserPlan(User user, Plan plan, LocalDateTime startDate) {
        UserPlan newUserPlan = UserPlan.builder()
                .id(new UserPlanId(null, plan.getId()))
                .user(user)
                .plan(plan)
                .planStatus(PlanStatus.ACTIVE)
                .startedAt(startDate)
                .build();
        setFinishAtByPlanType(newUserPlan, startDate);
        user.getUserPlans().add(newUserPlan);
    }

    // ... outros métodos ...
    private void setFinishAtByPlanType(UserPlan userPlan, LocalDateTime now) {
        if (now == null) { // Para planos pausados que podem não ter data de início imediata
            userPlan.setFinishAt(null);
            return;
        }
        switch (userPlan.getPlan().getType()) {
            case FREE:
                userPlan.setFinishAt(now.plusHours(24));
                break;
            case MONTHLY:
                userPlan.setFinishAt(now.plusDays(30));
                break;
            case PARTNER:
                userPlan.setFinishAt(null); // Partner não tem data de fim
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
        } else {
            // Se não for permitido, garante que o DTO não sobrescreva para 'true'
            dto.setActive(promotion.getActive());
        }

        // Regra 2: Ativação da promoção e lógica de plano
        if (Boolean.TRUE.equals(dto.getActive()) && dto.getPlanId() != null) {
            handlePlanTransition(user, dto.getPlanId());
        }

        updatePromotionFields(promotion, dto);
        Promotion updatedPromotion = promotionRepository.save(promotion);

        PlanDto updatedPlanDto = userPlanRepository.findActivePlanByUser(user, PlanStatus.ACTIVE)
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