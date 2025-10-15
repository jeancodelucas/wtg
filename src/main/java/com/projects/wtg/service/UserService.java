// src/main/java/com/projects/wtg/service/UserService.java

package com.projects.wtg.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.projects.wtg.dto.PromotionDataDto;
import com.projects.wtg.dto.UserRegistrationDto;
import com.projects.wtg.dto.UserUpdateDto;
import com.projects.wtg.exception.EmailAlreadyExistsException;
import com.projects.wtg.model.*;
import com.projects.wtg.repository.AccountRepository;
import com.projects.wtg.repository.PlanRepository;
import com.projects.wtg.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PlanRepository planRepository;
    private final EmailService emailService;
    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    private static final Pattern STRONG_PASSWORD_PATTERN =
            Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$");

    public UserService(UserRepository userRepository, AccountRepository accountRepository, @Lazy PasswordEncoder passwordEncoder, PlanRepository planRepository, EmailService emailService) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
        this.planRepository = planRepository;
        this.emailService = emailService;
    }

    @Transactional
    public void initiateRegistration(String email) {
        accountRepository.findByEmail(email).ifPresent(account -> {
            if (account.getPassword() != null) {
                throw new EmailAlreadyExistsException("Este e-mail já está cadastrado. Por favor, faça o login.");
            }
        });

        String token = String.format("%04d", new Random().nextInt(10000));
        LocalDateTime expiration = LocalDateTime.now().plusMinutes(1).plusSeconds(30);

        Account account = accountRepository.findByEmail(email).orElse(new Account());
        account.setEmail(email);

        if (account.getUserName() == null || account.getUserName().isEmpty()) {
            account.setUserName(email.split("@")[0]);
        }

        account.setRegistrationToken(token);
        account.setRegistrationTokenExpiration(expiration);
        accountRepository.save(account);

        try {
            emailService.sendRegistrationTokenEmail(email, token);
            logger.info("Token de registo enviado com sucesso para: {}", email);
        } catch (Exception e) {
            logger.error("Falha ao enviar e-mail de token para {}. O erro foi: {}", email, e.getMessage());
        }
    }

    @Transactional
    public void validateRegistrationToken(String email, String token) {
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Conta não encontrada para o e-mail: " + email));

        if (account.getRegistrationToken() == null || !account.getRegistrationToken().equals(token)) {
            throw new IllegalArgumentException("Token inválido.");
        }

        if (account.getRegistrationTokenExpiration().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Token expirado.");
        }

        account.setEmailVerified(true);
        account.setRegistrationToken(null);
        account.setRegistrationTokenExpiration(null);
        accountRepository.save(account);
    }

    @Transactional
    @Scheduled(fixedRate = 90000)
    public void tokenCleanupScheduler() {
        accountRepository.findAll().forEach(account -> {
            if (account.getRegistrationToken() != null && account.getRegistrationTokenExpiration().isBefore(LocalDateTime.now())) {
                account.setRegistrationToken(null);
                account.setRegistrationTokenExpiration(null);
                accountRepository.save(account);
            }
        });
    }


    @Transactional
    public User updateUser(String email, UserUpdateDto userUpdateDto) {
        User user = findUserByEmail(email);
        user.setFirstName(userUpdateDto.getFirstName());
        user.setCpf(userUpdateDto.getCpf());
        user.setBirthday(userUpdateDto.getBirthday());
        user.setPronouns(userUpdateDto.getPronouns());
        return userRepository.save(user);
    }

    @Transactional
    public void generatePasswordResetToken(String email) {
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Conta não encontrada para o e-mail: " + email));

        String token = String.format("%04d", new Random().nextInt(10000));
        account.setToken(token);
        account.setRegistrationTokenExpiration(LocalDateTime.now().plusMinutes(10));
        accountRepository.save(account);

        emailService.sendPasswordResetTokenEmail(email, token);
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        Account account = accountRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Token de redefinição de senha inválido ou expirado."));

        if (account.getRegistrationTokenExpiration() == null || account.getRegistrationTokenExpiration().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Token expirado. Por favor, solicite um novo.");
        }

        if (!isPasswordStrong(newPassword)) {
            throw new IllegalArgumentException("A nova senha não atende aos critérios de segurança.");
        }

        account.setPassword(passwordEncoder.encode(newPassword));
        account.setToken(null);
        accountRepository.save(account);
    }


    @Transactional
    public User createUserWithAccount(UserRegistrationDto userRegistrationDto, Authentication authentication) {
        Account account = accountRepository.findByEmail(userRegistrationDto.getEmail())
                .orElseThrow(() -> new EntityNotFoundException("E-mail não verificado. Por favor, inicie o processo de registro."));

        if (account.getPassword() != null) {
            throw new EmailAlreadyExistsException("Este e-mail já está cadastrado. Por favor, faça o login.");
        }
        if (Boolean.FALSE.equals(account.getEmailVerified())) {
            throw new IllegalStateException("E-mail não verificado. Por favor, valide o token enviado.");
        }
        if (!isPasswordStrong(userRegistrationDto.getPassword())) {
            throw new IllegalArgumentException("A senha não atende aos critérios de segurança...");
        }
        if (!userRegistrationDto.getPassword().equals(userRegistrationDto.getConfirmPassword())) {
            throw new IllegalArgumentException("As senhas não coincidem.");
        }

        Plan planToAssign;
        if (userRegistrationDto.getPlanId() != null) {
            if (authentication == null || !authentication.isAuthenticated()) {
                throw new AccessDeniedException("Apenas administradores podem criar usuários com planos específicos.");
            }
            User adminUser = this.findUserByEmail(authentication.getName());
            if (adminUser.getUserType() != UserType.ADMIN) {
                throw new AccessDeniedException("Apenas administradores podem criar usuários com planos específicos.");
            }
            planToAssign = planRepository.findById(userRegistrationDto.getPlanId())
                    .orElseThrow(() -> new EntityNotFoundException("Plano com ID " + userRegistrationDto.getPlanId() + " não encontrado."));
        } else {
            planToAssign = planRepository.findByType(PlanType.FREE)
                    .orElseThrow(() -> new IllegalStateException("Plano 'FREE' não encontrado no banco de dados."));
        }

        User user = new User();
        user.setFullName(userRegistrationDto.getFullName());
        user.setFirstName(userRegistrationDto.getFirstName());
        user.setCpf(userRegistrationDto.getCpf());
        user.setPronouns(userRegistrationDto.getPronouns());
        user.setBirthday(userRegistrationDto.getBirthday());

        if (userRegistrationDto.getLatitude() != null && userRegistrationDto.getLongitude() != null) {
            Point userPoint = geometryFactory.createPoint(new Coordinate(userRegistrationDto.getLongitude(), userRegistrationDto.getLatitude()));
            user.setPoint(userPoint);
        }

        user.setAccount(account);

        account.setUserName(userRegistrationDto.getUserName());
        account.setPassword(passwordEncoder.encode(userRegistrationDto.getPassword()));
        account.setActive(true);

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

    @Transactional
    public void updateUserLocation(String email, Double latitude, Double longitude) {
        if (latitude != null && longitude != null) {
            User user = findUserByEmail(email);
            Point userPoint = geometryFactory.createPoint(new Coordinate(longitude, latitude));
            user.setPoint(userPoint);
            userRepository.save(user);
        }
    }

    @Transactional
    public Account processGoogleUser(GoogleIdToken.Payload payload, Double latitude, Double longitude) {
        String email = payload.getEmail();
        Optional<Account> optionalAccount = accountRepository.findByEmail(email);

        Account account;
        if (optionalAccount.isPresent()) {
            account = optionalAccount.get();
        } else {
            User user = new User();
            user.setFirstName((String) payload.get("given_name"));
            user.setFullName((String) payload.get("family_name"));
            user.setPictureUrl((String) payload.get("picture"));

            account = new Account();
            account.setEmail(email);
            account.setUserName(email);
            account.setLoginProvider("google");
            account.setActive(true);
            user.setAccount(account);

            assignFreePlanToUser(user);

            userRepository.save(user);
        }

        if (latitude != null && longitude != null) {
            updateUserLocation(account.getEmail(), latitude, longitude);
        }

        return account;
    }

    private void assignFreePlanToUser(User user) {
        planRepository.findByType(PlanType.FREE).ifPresent(plan -> {
            UserPlan userPlan = new UserPlan();
            // CORREÇÃO APLICADA: Sintaxe correta e definição das datas do plano
            userPlan.setId(new UserPlanId(null, plan.getId()));
            userPlan.setUser(user);
            userPlan.setPlan(plan);
            userPlan.setPlanStatus(PlanStatus.ACTIVE);
            LocalDateTime now = LocalDateTime.now();
            userPlan.setStartedAt(now);
            setFinishAtByPlanType(userPlan, now);
            user.getUserPlans().add(userPlan);
        });
    }
}