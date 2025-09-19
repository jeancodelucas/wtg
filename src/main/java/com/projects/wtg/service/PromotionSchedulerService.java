package com.projects.wtg.service;

import com.projects.wtg.model.*;
import com.projects.wtg.repository.PromotionRepository;
import com.projects.wtg.repository.UserPlanRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class PromotionSchedulerService {

    private static final Logger logger = LoggerFactory.getLogger(PromotionSchedulerService.class);

    private final UserPlanRepository userPlanRepository;
    private final PromotionRepository promotionRepository;

    public PromotionSchedulerService(UserPlanRepository userPlanRepository, PromotionRepository promotionRepository) {
        this.userPlanRepository = userPlanRepository;
        this.promotionRepository = promotionRepository;
    }

    /**
     * Tarefa agendada que executa a cada hora para gerenciar planos gratuitos e promoções.
     * A expressão cron "0 0 * * * *" significa "no minuto 0 de cada hora".
     */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void manageFreePlansAndPromotions() {
        logger.info("Iniciando verificação de planos gratuitos...");
        List<UserPlan> activeFreePlans = userPlanRepository.findAllByPlanTypeAndPlanStatus(PlanType.FREE, PlanStatus.ACTIVE);

        List<Promotion> promotionsToUpdate = new ArrayList<>();
        List<UserPlan> userPlansToUpdate = new ArrayList<>();

        LocalDateTime now = LocalDateTime.now();

        for (UserPlan userPlan : activeFreePlans) {
            User user = userPlan.getUser();

            if (userPlan.getFinishAt() == null) {
                // Caso 1: Expira o plano gratuito após 24 horas
                LocalDateTime expirationTime = userPlan.getStartedAt().plusHours(24);
                if (now.isAfter(expirationTime) || now.isEqual(expirationTime)) {
                    logger.info("Expirando plano gratuito para o usuário ID: {}", user.getId());
                    userPlan.setFinishAt(now);
                    userPlansToUpdate.add(userPlan);

                    if (user.getPromotions() != null) {
                        user.getPromotions().forEach(promo -> {
                            promo.setAllowUserActivePromotion(false);
                            promo.setActive(false);
                            promotionsToUpdate.add(promo);
                        });
                    }
                }
            } else {
                // Caso 2: Reativa as promoções após 7 dias de "cooldown"
                LocalDateTime reactivationTime = userPlan.getFinishAt().plusDays(7);
                if (now.isAfter(reactivationTime) || now.isEqual(reactivationTime)) {
                    logger.info("Reativando promoções para o usuário ID: {}", user.getId());
                    userPlan.setFinishAt(null); // Limpa o campo para o ciclo recomeçar
                    userPlansToUpdate.add(userPlan);

                    if (user.getPromotions() != null) {
                        user.getPromotions().forEach(promo -> {
                            promo.setAllowUserActivePromotion(true);
                            // Você pode decidir se quer reativar a promoção ou não
                            // promo.setActive(true);
                            promotionsToUpdate.add(promo);
                        });
                    }
                }
            }
        }

        if (!userPlansToUpdate.isEmpty()) {
            userPlanRepository.saveAll(userPlansToUpdate);
            logger.info("{} planos de usuário foram atualizados.", userPlansToUpdate.size());
        }

        if (!promotionsToUpdate.isEmpty()) {
            promotionRepository.saveAll(promotionsToUpdate);
            logger.info("{} promoções foram atualizadas.", promotionsToUpdate.size());
        }

        logger.info("Verificação de planos gratuitos concluída.");
    }
}