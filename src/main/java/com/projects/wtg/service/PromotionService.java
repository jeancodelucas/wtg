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

        // CORREÇÃO: O método handlePromotionActivation agora retorna a mensagem a ser exibida.
        String message = handlePromotionActivation(user, promotion, dto.getActive(), dto.getPlanId());

        Promotion updatedPromotion = promotionRepository.save(promotion);
        PlanDto updatedPlanDto = userPlanRepository.findTopByUserOrderByCreatedAtDesc(user).map(PlanDto::new).orElse(null);

        return PromotionEditResponseDto.builder()
                .promotion(new PromotionDto(updatedPromotion))
                .userPlan(updatedPlanDto)
                .message(message) // A mensagem agora é dinâmica
                .build();
    }

    private String handlePromotionActivation(User user, Promotion promotion, Boolean active, Long planId) {
        promotion.setActive(active);
        String message = "Promoção atualizada com sucesso.";

        if (Boolean.FALSE.equals(active)) {
            return message;
        }

        // --- LÓGICA DE ATIVAÇÃO COM VERIFICAÇÃO DE PLANO EXISTENTE ---

        if (planId != null) {
            // CORREÇÃO: Verifica se o usuário já possui o plano (ativo ou agendado)
            boolean planAlreadyExists = user.getUserPlans().stream()
                    .anyMatch(up -> up.getPlan().getId().equals(planId) &&
                            (up.getPlanStatus() == PlanStatus.ACTIVE || up.getPlanStatus() == PlanStatus.READYTOACTIVE));

            if (planAlreadyExists) {
                return "Promoção atualizada com sucesso. O plano não foi alterado pois o usuário já possui este plano ativo ou agendado.";
            }

            Plan planToAssign = planRepository.findById(planId)
                    .orElseThrow(() -> new EntityNotFoundException("Plano com ID " + planId + " não encontrado."));

            Optional<UserPlan> activePlanOpt = userPlanRepository.findByUserAndPlanStatus(user, PlanStatus.ACTIVE);

            if (activePlanOpt.isPresent()) {
                UserPlan currentActivePlan = activePlanOpt.get();
                if (currentActivePlan.getFinishAt() == null) {
                    return "Promoção atualizada, mas não foi possível agendar o novo plano pois o plano atual não possui data de término.";
                }

                UserPlan futurePlan = new UserPlan();
                futurePlan.setId(new UserPlanId(null, planToAssign.getId()));
                futurePlan.setUser(user);
                futurePlan.setPlan(planToAssign);
                futurePlan.setStartedAt(currentActivePlan.getFinishAt());
                setFinishAtByPlanType(futurePlan, futurePlan.getStartedAt());
                futurePlan.setPlanStatus(PlanStatus.READYTOACTIVE);
                user.getUserPlans().add(futurePlan);

            } else {
                UserPlan newUserPlan = new UserPlan();
                newUserPlan.setId(new UserPlanId(null, planToAssign.getId()));
                newUserPlan.setUser(user);
                newUserPlan.setPlan(planToAssign);
                newUserPlan.setStartedAt(LocalDateTime.now());
                setFinishAtByPlanType(newUserPlan, newUserPlan.getStartedAt());
                newUserPlan.setPlanStatus(PlanStatus.ACTIVE);
                user.getUserPlans().add(newUserPlan);
            }
        } else {
            Optional<UserPlan> readyPlanOpt = userPlanRepository.findByUserAndPlanStatus(user, PlanStatus.READYTOACTIVE);
            if (readyPlanOpt.isPresent()) {
                UserPlan planToActivate = readyPlanOpt.get();
                LocalDateTime now = LocalDateTime.now();
                planToActivate.setPlanStatus(PlanStatus.ACTIVE);
                planToActivate.setStartedAt(now);
                setFinishAtByPlanType(planToActivate, now);
            } else {
                Optional<UserPlan> existingPlanOpt = userPlanRepository.findActiveOrFuturePausedPlan(user, LocalDateTime.now());
                if (existingPlanOpt.isEmpty()) {
                    Plan freePlan = planRepository.findByType(PlanType.FREE)
                            .orElseThrow(() -> new IllegalStateException("Plano 'FREE' não encontrado no banco de dados."));
                    createNewUserPlan(user, freePlan, LocalDateTime.now());
                }
            }
        }
        return message;
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
        if (startDate == null) {
            userPlan.setFinishAt(null);
            return;
        }
        switch (userPlan.getPlan().getType()) {
            case FREE:
                userPlan.setFinishAt(startDate.plusHours(24));
                break;
            case MONTHLY:
                userPlan.setFinishAt(startDate.plusDays(30));
                break;
            case PARTNER:
                userPlan.setFinishAt(startDate.plusYears(1));
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

        if (dto.getAddress() != null) {
            Address address = promotion.getAddress();
            if (address == null) {
                address = new Address();
                promotion.setAddress(address);
            }
            address.setAddress(dto.getAddress().getAddress());
            address.setNumber(dto.getAddress().getNumber());
            address.setComplement(dto.getAddress().getComplement());
            address.setReference(dto.getAddress().getReference());
            address.setPostalCode(dto.getAddress().getPostalCode());
            address.setObs(dto.getAddress().getObs());
        }
    }
}