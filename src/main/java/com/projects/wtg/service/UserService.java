package com.projects.wtg.service;

import com.projects.wtg.dto.PromotionDataDto;
import com.projects.wtg.dto.UserRegistrationDto;
import com.projects.wtg.exception.EmailAlreadyExistsException;
import com.projects.wtg.model.*;
import com.projects.wtg.repository.AccountRepository;
import com.projects.wtg.repository.PlanRepository;
import com.projects.wtg.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
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

    public UserService(UserRepository userRepository, AccountRepository accountRepository, @Lazy PasswordEncoder passwordEncoder, PlanRepository planRepository) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
        this.planRepository = planRepository;
    }

    @Transactional
    public User createUserWithAccount(UserRegistrationDto userRegistrationDto, Authentication authentication) {
        if (userRegistrationDto.getPlanId() != null) {
            if (authentication == null || !authentication.isAuthenticated()) {
                throw new AccessDeniedException("Apenas administradores podem criar usuários com planos específicos.");
            }
            String adminEmail = authentication.getName();
            User adminUser = this.findUserByEmail(adminEmail);
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

        // --- LÓGICA DE PLANO E PROMOÇÃO CORRIGIDA ---

        Plan planToAssign;
        if (userRegistrationDto.getPlanId() != null) {
            planToAssign = planRepository.findById(userRegistrationDto.getPlanId())
                    .orElseThrow(() -> new EntityNotFoundException("Plano com ID " + userRegistrationDto.getPlanId() + " não encontrado."));
        } else {
            planToAssign = planRepository.findByType(PlanType.FREE)
                    .orElseThrow(() -> new IllegalStateException("Plano 'FREE' não encontrado no banco de dados."));
        }

        UserPlan newUserPlan = new UserPlan();
        newUserPlan.setId(new UserPlanId(null, planToAssign.getId()));
        newUserPlan.setUser(user);
        newUserPlan.setPlan(planToAssign);

        boolean activateNow = userRegistrationDto.getPromotion() != null &&
                Boolean.TRUE.equals(userRegistrationDto.getPromotion().getActive());

        if (activateNow) {
            LocalDateTime now = LocalDateTime.now();
            newUserPlan.setStartedAt(now);
            setFinishAtByPlanType(newUserPlan, now);
            newUserPlan.setPlanStatus(PlanStatus.ACTIVE);
        } else {
            newUserPlan.setStartedAt(null);
            newUserPlan.setFinishAt(null);
            newUserPlan.setPlanStatus(PlanStatus.READYTOACTIVE);
        }
        user.getUserPlans().add(newUserPlan);

        if (userRegistrationDto.getPromotion() != null) {
            Promotion promotion = buildPromotionFromDto(userRegistrationDto.getPromotion());
            promotion.setAllowUserActivePromotion(true);
            user.addPromotion(promotion);
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