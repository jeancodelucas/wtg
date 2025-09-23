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

        if (Boolean.TRUE.equals(dto.getActive())) {
            handleUserPlanActivation(user);
        }

        return userRepository.save(user);
    }

    private void handleUserPlanActivation(User user) {
        // Busca o plano que está explicitamente marcado como ATIVO
        Optional<UserPlan> activeUserPlanOpt = userPlanRepository.findActivePlanByUser(user, PlanStatus.ACTIVE);
        LocalDateTime now = LocalDateTime.now();

        if (activeUserPlanOpt.isEmpty()) {
            // REGRA 1.1: Se não existe plano ATIVO, cria um novo com o plano FREE.
            Plan freePlan = planRepository.findByType(PlanType.FREE)
                    .orElseThrow(() -> new IllegalStateException("Plano 'FREE' não encontrado no banco de dados."));

            UserPlan newUserPlan = UserPlan.builder()
                    .id(new UserPlanId(user.getId(), freePlan.getId()))
                    .user(user)
                    .plan(freePlan)
                    .planStatus(PlanStatus.ACTIVE)
                    .paymentMade(true)
                    .startedAt(now)
                    .finishAt(now.plusHours(24))
                    .build();
            user.getUserPlans().add(newUserPlan);
        } else {
            // REGRAS 1.2 e 1.3: Se já existe um plano ATIVO.
            UserPlan activeUserPlan = activeUserPlanOpt.get();
            if (activeUserPlan.getStartedAt() == null && activeUserPlan.getFinishAt() == null) {
                // REGRA 1.2
                activeUserPlan.setStartedAt(now);
                setFinishAtByPlanType(activeUserPlan, now);
            } else if (activeUserPlan.getStartedAt() != null && activeUserPlan.getFinishAt() == null) {
                // REGRA 1.3
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
                // Não faz nada com finishAt, deixando-o nulo
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

        UserPlan userPlan = userPlanRepository.findActivePlanByUser(user, PlanStatus.ACTIVE)
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