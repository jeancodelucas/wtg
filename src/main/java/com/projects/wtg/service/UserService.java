package com.projects.wtg.service;

import com.projects.wtg.dto.UserRegistrationDto;
import com.projects.wtg.exception.EmailAlreadyExistsException;
import com.projects.wtg.model.*;
import com.projects.wtg.repository.AccountRepository;
import com.projects.wtg.repository.PlanRepository;
import com.projects.wtg.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
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

    public UserService(UserRepository userRepository, AccountRepository accountRepository, PasswordEncoder passwordEncoder, PlanRepository planRepository) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
        this.planRepository = planRepository;
    }

    @Transactional
    public User createUserWithAccount(UserRegistrationDto userRegistrationDto) {
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
        account.setActive(true); // Define a CONTA como ativa no cadastro

        user.setAccount(account);
        assignFreePlanToUser(user);
        return userRepository.save(user);
    }

    @Transactional
    public User deactivateUserByEmail(String email) {
        Account account = accountRepository.findByEmailWithUserAndPlans(email)
                .orElseThrow(() -> new UsernameNotFoundException("Conta não encontrada para o e-mail: " + email));

        account.setActive(false); // Desativa a CONTA
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
}