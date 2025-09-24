package com.projects.wtg.service;

import com.projects.wtg.model.*;
import com.projects.wtg.repository.UserPlanRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PromotionSchedulerService {

    private static final Logger logger = LoggerFactory.getLogger(PromotionSchedulerService.class);

    private final UserPlanRepository userPlanRepository;

    public PromotionSchedulerService(UserPlanRepository userPlanRepository) {
        this.userPlanRepository = userPlanRepository;
    }

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void manageUserPlans() {
        logger.info("Iniciando tarefa de gerenciamento de planos de usuário...");
        LocalDateTime now = LocalDateTime.now();

        expireActivePlans(now);
        activateReadyPlans(now);

        logger.info("Gerenciamento de planos de usuário concluído.");
    }

    private void expireActivePlans(LocalDateTime now) {
        List<UserPlan> expiredPlans = userPlanRepository.findExpiredActivePlans(now);
        if (expiredPlans.isEmpty()) {
            return;
        }

        logger.info("Encontrados {} planos expirados para desativar.", expiredPlans.size());

        for (UserPlan userPlan : expiredPlans) {
            userPlan.setPlanStatus(PlanStatus.INACTIVE);

            if (userPlan.getUser().getPromotions() != null) {
                userPlan.getUser().getPromotions().forEach(promo -> {
                    promo.setActive(false);
                    promo.setAllowUserActivePromotion(false);
                });
            }
        }
        userPlanRepository.saveAll(expiredPlans);
    }

    private void activateReadyPlans(LocalDateTime now) {
        List<UserPlan> plansReadyForCheck = userPlanRepository.findReadyToActivatePlans(now);
        if (plansReadyForCheck.isEmpty()) {
            return;
        }

        logger.info("Encontrados {} planos prontos para verificação.", plansReadyForCheck.size());

        Map<User, List<UserPlan>> plansByUser = plansReadyForCheck.stream()
                .collect(Collectors.groupingBy(UserPlan::getUser));

        List<UserPlan> plansToSave = new ArrayList<>();

        for (Map.Entry<User, List<UserPlan>> entry : plansByUser.entrySet()) {
            User user = entry.getKey();
            List<UserPlan> readyPlans = entry.getValue();

            boolean hasActivePlan = user.getUserPlans().stream()
                    .anyMatch(p -> p.getPlanStatus() == PlanStatus.ACTIVE);

            if (hasActivePlan) {
                continue;
            }

            Optional<UserPlan> planToProcessOpt = readyPlans.stream()
                    .min(Comparator.comparing(UserPlan::getStartedAt));

            if (planToProcessOpt.isPresent()) {
                UserPlan planToProcess = planToProcessOpt.get();

                // --- CORREÇÃO: Adicionada a verificação do finishAt ---
                // Se o plano já tem uma data de término e essa data já passou, ele é inativado.
                if (planToProcess.getFinishAt() != null && planToProcess.getFinishAt().isBefore(now)) {
                    planToProcess.setPlanStatus(PlanStatus.INACTIVE);
                    logger.info("Plano ID {} para o usuário ID {} foi inativado pois sua data de término já passou.", planToProcess.getPlan().getId(), user.getId());
                } else {
                    // Caso contrário, o plano é ativado.
                    planToProcess.setPlanStatus(PlanStatus.ACTIVE);

                    if (planToProcess.getStartedAt() == null || planToProcess.getStartedAt().isBefore(now)) {
                        planToProcess.setStartedAt(now);
                    }

                    setFinishAtByPlanType(planToProcess, planToProcess.getStartedAt());

                    if (user.getPromotions() != null) {
                        user.getPromotions().forEach(promo -> promo.setAllowUserActivePromotion(true));
                    }
                    logger.info("Plano ID {} ativado para o usuário ID: {}", planToProcess.getPlan().getId(), user.getId());
                }
                plansToSave.add(planToProcess);
            }
        }

        if (!plansToSave.isEmpty()) {
            userPlanRepository.saveAll(plansToSave);
        }
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
                userPlan.setFinishAt(null);
                break;
        }
    }
}