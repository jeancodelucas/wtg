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
import java.util.Optional;

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
        logger.info(">>> Método OIDC loadUser iniciado. Buscando informações do usuário...");
        OidcUser oidcUser = super.loadUser(userRequest);
        Map<String, Object> attributes = oidcUser.getAttributes();

        String email = (String) attributes.get("email");
        logger.info(">>> Email do usuário OIDC: {}", email);

        String given_name = (String) attributes.get("given_name");
        String family_name = (String) attributes.get("family_name");
        String picture = (String) attributes.get("picture");
        String provider = userRequest.getClientRegistration().getRegistrationId();
        String sub = (String) attributes.get("sub");

        Optional<Account> existingAccount = accountRepository.findByEmail(email);
        User user;

        if (existingAccount.isPresent()) {
            logger.info(">>> Usuário existente encontrado. Atualizando dados...");
            Account account = existingAccount.get();
            user = account.getUser();

            user.setFirstName(given_name);
            user.setFullName(family_name);
            user.setPictureUrl(picture);
            account.setLoginSub(sub);
            account.setLoginProvider(provider);

        } else {
            logger.info(">>> Nenhum usuário encontrado. Criando novo usuário...");
            user = new User();
            user.setFirstName(given_name);
            user.setFullName(family_name);
            user.setPictureUrl(picture);
            // CORREÇÃO: Removemos o set manual de createdAt e updatedAt. O JPA Auditing fará isso.

            Account account = new Account();
            account.setEmail(email);
            account.setUserName(email);
            account.setLoginSub(sub);
            account.setLoginProvider(provider);
            // CORREÇÃO: Removemos o set manual de createdAt e updatedAt.

            user.setAccount(account);

            // CORREÇÃO: Adicionando a atribuição do plano gratuito para novos usuários SSO.
            assignFreePlanToUser(user);
        }

        userRepository.save(user);

        logger.info(">>> Dados do usuário OIDC salvos/atualizados com sucesso!");
        return oidcUser;
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