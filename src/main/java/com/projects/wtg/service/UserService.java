package com.projects.wtg.service;

import com.projects.wtg.dto.UserRegistrationDto;
import com.projects.wtg.exception.EmailAlreadyExistsException;
import com.projects.wtg.model.*;
import com.projects.wtg.repository.AccountRepository;
import com.projects.wtg.repository.PlanRepository;
import com.projects.wtg.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
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

    public UserService(UserRepository userRepository, AccountRepository accountRepository, PasswordEncoder passwordEncoder, PlanRepository planRepository) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
        this.planRepository = planRepository;
    }

    @Transactional
    public User createUserWithAccount(UserRegistrationDto userRegistrationDto) {
        // ... (código existente para criar usuário)
        accountRepository.findByEmail(userRegistrationDto.getEmail())
                .ifPresent(account -> {
                    throw new EmailAlreadyExistsException("Este e-mail já está cadastrado. Por favor, faça o login.");
                });

        if (!isPasswordStrong(userRegistrationDto.getPassword())) {
            throw new IllegalArgumentException("A senha não atende aos critérios de segurança (mínimo 8 caracteres, com maiúscula, minúscula, número e caractere especial).");
        }

        if (!userRegistrationDto.getPassword().equals(userRegistrationDto.getConfirmPassword())) {
            throw new IllegalArgumentException("As senhas não coincidem.");
        }

        User user = new User();
        user.setFullName(userRegistrationDto.getFullName());
        user.setFirstName(userRegistrationDto.getFirstName());
        user.setActive(true); // Define o usuário como ativo no momento do cadastro

        Account account = new Account();
        account.setUserName(userRegistrationDto.getUserName());
        account.setEmail(userRegistrationDto.getEmail());
        account.setPassword(passwordEncoder.encode(userRegistrationDto.getPassword()));

        user.setAccount(account);

        assignFreePlanToUser(user);

        return userRepository.save(user);
    }

    @Transactional
    public void deleteUserById(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new EntityNotFoundException("Usuário com ID " + userId + " não encontrado.");
        }
        userRepository.deleteById(userId);
    }

    @Transactional
    public User deactivateUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Usuário com ID " + userId + " não encontrado."));

        user.setActive(false);
        return userRepository.save(user);
    }

    private void assignFreePlanToUser(User user) {
        // ... (código existente para atribuir plano)
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
        // ... (código existente para validar senha)
        if (password == null) {
            return false;
        }
        return STRONG_PASSWORD_PATTERN.matcher(password).matches();
    }
}