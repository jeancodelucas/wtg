package com.projects.wtg.service;

import com.projects.wtg.dto.*;
import com.projects.wtg.exception.PromotionAlreadyExistsException;
import com.projects.wtg.model.*;
import com.projects.wtg.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import java.util.List;

import java.time.LocalDateTime;
import java.util.Objects;
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

        String message = "Promoção atualizada com sucesso.";

        boolean statusChangeAttempted = !Objects.equals(dto.getActive(), promotion.getActive());

        if (statusChangeAttempted) {
            if (Boolean.FALSE.equals(promotion.getAllowUserActivePromotion())) {
                message += " No entanto, o status do evento não pode ser alterado no momento. Aguarde a reativação do seu plano.";
            } else {
                message = handlePromotionActivation(user, promotion, dto.getActive(), dto.getPlanId());
            }
        }

        Promotion updatedPromotion = promotionRepository.save(promotion);
        PlanDto updatedPlanDto = userPlanRepository.findTopByUserOrderByCreatedAtDesc(user).map(PlanDto::new).orElse(null);

        return PromotionEditResponseDto.builder()
                .promotion(new PromotionDto(updatedPromotion))
                .userPlan(updatedPlanDto)
                .message(message)
                .build();
    }

    private String handlePromotionActivation(User user, Promotion promotion, Boolean active, Long planId) {
        promotion.setActive(active);
        String message = "Promoção atualizada com sucesso.";

        if (Boolean.FALSE.equals(active)) {
            return message;
        }

        if (planId != null) {
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
                UserPlan newUserPlan = createNewUserPlan(user, planToAssign, LocalDateTime.now());
                // --- CORREÇÃO ADICIONADA AQUI ---
                // Sempre que um novo plano é ativado, a permissão deve ser concedida.
                if (user.getPromotions() != null) {
                    user.getPromotions().forEach(p -> p.setAllowUserActivePromotion(true));
                }
            }
        } else {
            Optional<UserPlan> readyPlanOpt = userPlanRepository.findByUserAndPlanStatus(user, PlanStatus.READYTOACTIVE);
            if (readyPlanOpt.isPresent()) {
                UserPlan planToActivate = readyPlanOpt.get();
                LocalDateTime now = LocalDateTime.now();
                planToActivate.setPlanStatus(PlanStatus.ACTIVE);
                planToActivate.setStartedAt(now);
                setFinishAtByPlanType(planToActivate, now);
                // --- CORREÇÃO ADICIONADA AQUI ---
                if (user.getPromotions() != null) {
                    user.getPromotions().forEach(p -> p.setAllowUserActivePromotion(true));
                }
            } else {
                Optional<UserPlan> existingPlanOpt = userPlanRepository.findActiveOrFuturePausedPlan(user, LocalDateTime.now());
                if (existingPlanOpt.isEmpty()) {
                    Plan freePlan = planRepository.findByType(PlanType.FREE)
                            .orElseThrow(() -> new IllegalStateException("Plano 'FREE' não encontrado no banco de dados."));
                    createNewUserPlan(user, freePlan, LocalDateTime.now());
                    // --- CORREÇÃO ADICIONADA AQUI ---
                    if (user.getPromotions() != null) {
                        user.getPromotions().forEach(p -> p.setAllowUserActivePromotion(true));
                    }
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
    public List<Promotion> findNearby(double latitude, double longitude, double radiusInKm) {
        // O GeometryFactory é usado para criar objetos geométricos.
        // O SRID 4326 corresponde ao sistema de coordenadas WGS 84.
        GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

        // Cria um objeto Point a partir das coordenadas do usuário.
        Point userLocation = geometryFactory.createPoint(new Coordinate(longitude, latitude));

        // Converte o raio de quilômetros para metros, pois ST_DWithin trabalha com metros.
        double radiusInMeters = radiusInKm * 1000;

        // Chama o método do repositório para buscar as promoções.
        return promotionRepository.findPromotionsWithinRadius(userLocation, radiusInMeters);
    }
}