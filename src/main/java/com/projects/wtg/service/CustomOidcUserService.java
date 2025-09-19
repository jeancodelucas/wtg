package com.projects.wtg.service;

import com.projects.wtg.model.*;
import com.projects.wtg.repository.AccountRepository;
import com.projects.wtg.repository.PlanRepository;
import com.projects.wtg.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class CustomOidcUserService extends OidcUserService {

    private static final Logger logger = LoggerFactory.getLogger(CustomOidcUserService.class);

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final PlanRepository planRepository;

    public CustomOidcUserService(UserRepository userRepository, AccountRepository accountRepository, PlanRepository planRepository) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.planRepository = planRepository;
    }

    @Override
    @Transactional
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);
        Map<String, Object> attributes = oidcUser.getAttributes();
        String email = (String) attributes.get("email");

        // Refatorado para usar orElseGet, que é mais limpo
        User user = accountRepository.findByEmail(email)
                .map(Account::getUser)
                .orElseGet(() -> createNewSsoUser(attributes, userRequest.getClientRegistration().getRegistrationId()));

        updateSsoUserData(user, attributes);

        userRepository.save(user);
        logger.info(">>> Dados do usuário OIDC (email: {}) salvos/atualizados com sucesso!", email);
        return oidcUser;
    }

    // Método privado para criar um novo usuário SSO
    private User createNewSsoUser(Map<String, Object> attributes, String provider) {
        logger.info(">>> Nenhum usuário encontrado para o e-mail: {}. Criando novo usuário...", attributes.get("email"));
        User user = new User();

        Account account = new Account();
        account.setEmail((String) attributes.get("email"));
        account.setUserName((String) attributes.get("email"));
        account.setLoginProvider(provider);
        account.setActive(true); // Define a conta como ativa

        user.setAccount(account);

        // CORREÇÃO: Adicionando a atribuição do plano gratuito para novos usuários SSO.
        assignFreePlanToUser(user);
        return user;
    }

    // Método privado para atualizar os dados do usuário SSO
    private void updateSsoUserData(User user, Map<String, Object> attributes) {
        user.setFirstName((String) attributes.get("given_name"));
        user.setFullName((String) attributes.get("family_name"));
        user.setPictureUrl((String) attributes.get("picture"));
        if (user.getAccount() != null) {
            user.getAccount().setLoginSub((String) attributes.get("sub"));
        }
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
}