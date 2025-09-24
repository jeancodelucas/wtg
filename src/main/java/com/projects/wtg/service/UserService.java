package com.projects.wtg.service;

import com.projects.wtg.dto.PromotionDataDto;
import com.projects.wtg.dto.UserRegistrationDto;
import com.projects.wtg.exception.EmailAlreadyExistsException;
import com.projects.wtg.model.*;
import com.projects.wtg.repository.AccountRepository;
import com.projects.wtg.repository.PlanRepository;
import com.projects.wtg.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.context.annotation.Lazy; // 1. Importe a anotação @Lazy
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.regex.Pattern;

@Service
public class UserService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PlanRepository planRepository;

    private static final Pattern STRONG_PASSWORD_PATTERN =
            Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$");

    // 2. Adicione a anotação @Lazy no parâmetro que fecha o ciclo
    public UserService(UserRepository userRepository, AccountRepository accountRepository, @Lazy PasswordEncoder passwordEncoder, PlanRepository planRepository) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
        this.planRepository = planRepository;
    }

    @Transactional
    public User createUserWithAccount(UserRegistrationDto userRegistrationDto, Authentication authentication) {
        // REGRA DE ADMIN: Se um planId é fornecido, verifica se o autor da requisição é admin.
        if (userRegistrationDto.getPlanId() != null) {
            if (authentication == null || !authentication.isAuthenticated()) {
                throw new AccessDeniedException("Apenas administradores podem criar usuários com planos específicos.");
            }
            String adminEmail = authentication.getName();
            User adminUser = this.findUserByEmail(adminEmail); // Usa o método existente na classe
            if (adminUser.getUserType() != UserType.ADMIN) {
                throw new AccessDeniedException("Apenas administradores podem criar usuários com planos específicos.");
            }
        }

        accountRepository.findByEmail(userRegistrationDto.getEmail())
                .ifPresent(account -> {
                    throw new EmailAlreadyExistsException("Este e-mail já está cadastrado. Por favor, faça o login.");
                });

        if (!isPasswordStrong(userRegistrationDto.getPassword())) {
            throw new IllegalArgumentException("A senha não atende aos critérios de segurança...");
        }

        if (!userRegistrationDto.getPassword().equals(userRegistrationDto.getConfirmPassword())) {
            throw new IllegalArgumentException("As senhas não coincidem.");
        }

        User user = new User();
        user.setFullName(userRegistrationDto.getFullName());
        user.setFirstName(userRegistrationDto.getFirstName());

        Account account = new Account();
        account.setUserName(userRegistrationDto.getUserName());
        account.setEmail(userRegistrationDto.getEmail());
        account.setPassword(passwordEncoder.encode(userRegistrationDto.getPassword()));
        account.setActive(true);

        user.setAccount(account);

        if (userRegistrationDto.getPromotion() != null) {
            Promotion promotion = buildPromotionFromDto(userRegistrationDto.getPromotion());
            user.addPromotion(promotion);

            boolean promotionIsActive = userRegistrationDto.getPromotion().getActive();
            Long planId = userRegistrationDto.getPlanId();
            Plan planToAssign;
            UserPlan newUserPlan = new UserPlan();

            if (planId == null) {
                planToAssign = planRepository.findByType(PlanType.FREE)
                        .orElseThrow(() -> new IllegalStateException("Plano 'FREE' não encontrado no banco de dados."));
            } else {
                planToAssign = planRepository.findById(planId)
                        .orElseThrow(() -> new EntityNotFoundException("Plano com ID " + planId + " não encontrado."));
            }

            newUserPlan.setId(new UserPlanId(null, planToAssign.getId()));
            newUserPlan.setUser(user);
            newUserPlan.setPlan(planToAssign);

            if (promotionIsActive) {
                LocalDateTime now = LocalDateTime.now();
                newUserPlan.setStartedAt(now);
                setFinishAtByPlanType(newUserPlan, now);
                newUserPlan.setPlanStatus(PlanStatus.ACTIVE);
                promotion.setAllowUserActivePromotion(true);
            } else {
                newUserPlan.setStartedAt(null);
                newUserPlan.setFinishAt(null);
                newUserPlan.setPlanStatus(PlanStatus.READYTOACTIVE);
                promotion.setAllowUserActivePromotion(true);
            }
            user.getUserPlans().add(newUserPlan);

        } else if (userRegistrationDto.getPlanId() != null) {
            assignSpecificPlanToUser(user, userRegistrationDto.getPlanId());
        } else {
            assignFreePlanToUser(user);
        }

        return userRepository.save(user);
    }
    private Promotion buildPromotionFromDto(PromotionDataDto dto) {
        Promotion promotion = new Promotion();
        promotion.setTitle(dto.getTitle());
        promotion.setDescription(dto.getDescription());
        promotion.setFree(dto.isFree());
        promotion.setObs(dto.getObs());
        promotion.setActive(dto.getActive());

        if (dto.getAddress() != null) {
            Address address = new Address();
            address.setAddress(dto.getAddress().getAddress());
            address.setNumber(dto.getAddress().getNumber());
            address.setComplement(dto.getAddress().getComplement());
            address.setReference(dto.getAddress().getReference());
            address.setPostalCode(dto.getAddress().getPostalCode());
            address.setObs(dto.getAddress().getObs());
            promotion.setAddress(address);
        }

        return promotion;
    }


    private void assignSpecificPlanToUser(User user, Long planId) {
        LocalDateTime now = LocalDateTime.now();
        Plan planToAssign = planRepository.findById(planId)
                .orElseThrow(() -> new EntityNotFoundException("Plano com ID " + planId + " não encontrado."));

        UserPlan newUserPlan = UserPlan.builder()
                .id(new UserPlanId(null, planToAssign.getId()))
                .user(user)
                .plan(planToAssign)
                .planStatus(PlanStatus.ACTIVE)
                .startedAt(now)
                .build();

        setFinishAtByPlanType(newUserPlan, now);
        user.getUserPlans().add(newUserPlan);
    }

    @Transactional
    public User deactivateUserByEmail(String email) {
        Account account = accountRepository.findByEmailWithUserAndPlans(email)
                .orElseThrow(() -> new UsernameNotFoundException("Conta não encontrada para o e-mail: " + email));

        account.setActive(false);
        accountRepository.save(account);

        return account.getUser();
    }

    @Transactional
    public void deleteUserByEmail(String email) {
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Conta não encontrada para o e-mail: " + email));

        userRepository.delete(account.getUser());
    }

    @Transactional
    public Account reactivateAccount(Account account) {
        if (account != null && !account.getActive()) {
            account.setActive(true);
            return accountRepository.save(account);
        }
        return account;
    }

    private void assignFreePlanToUser(User user) {
        Plan freePlan = planRepository.findByType(PlanType.FREE)
                .orElseThrow(() -> new IllegalStateException("Plano 'FREE' não encontrado no banco de dados."));

        UserPlan userPlan = UserPlan.builder()
                .id(new UserPlanId(null, freePlan.getId()))
                .user(user)
                .plan(freePlan)
                .planStatus(PlanStatus.ACTIVE)
                .startedAt(LocalDateTime.now())
                .build();

        user.getUserPlans().add(userPlan);
    }

    private boolean isPasswordStrong(String password) {
        if (password == null) {
            return false;
        }
        return STRONG_PASSWORD_PATTERN.matcher(password).matches();
    }

    private void setFinishAtByPlanType(UserPlan userPlan, LocalDateTime now) {
        if (now == null) {
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
                userPlan.setFinishAt(now.plusYears(1));
                break;
            default:
                break;
        }
    }

    @Transactional(readOnly = true)
    public User findUserByEmail(String email) {
        return accountRepository.findByEmailWithUserAndPlans(email)
                .map(Account::getUser)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado com o e-mail: " + email));
    }
}