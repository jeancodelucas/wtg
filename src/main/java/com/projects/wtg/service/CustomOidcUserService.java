// src/main/java/com/projects/wtg/service/CustomOidcUserService.java

package com.projects.wtg.service;

import com.projects.wtg.model.Account;
import com.projects.wtg.model.User;
import com.projects.wtg.repository.AccountRepository;
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

    public CustomOidcUserService(UserRepository userRepository, AccountRepository accountRepository) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
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

            // Lógica de atualização
            user.setFirstName(given_name);
            user.setFullName(family_name);
            user.setPictureUrl(picture);
            account.setLoginSub(sub);
            account.setLoginProvider(provider);
            // ... outros campos de atualização

        } else {
            logger.info(">>> Nenhum usuário encontrado. Criando novo usuário...");
            user = new User();
            user.setFirstName(given_name);
            user.setFullName(family_name);
            user.setPictureUrl(picture);
            user.setCreatedAt(LocalDateTime.now());
            user.setUpdatedAt(LocalDateTime.now());

            Account account = new Account();
            account.setEmail(email);
            account.setUserName(email); // Usando o email como username padrão
            account.setLoginSub(sub);
            account.setLoginProvider(provider);
            account.setCreatedAt(LocalDateTime.now());
            account.setUpdatedAt(LocalDateTime.now());

            // Relacionamento bidirecional
            user.setAccount(account);
            account.setUser(user);
        }

        // LINHA CRÍTICA QUE ESTAVA FALTANDO
        userRepository.save(user);

        logger.info(">>> Dados do usuário OIDC salvos/atualizados com sucesso!");
        return oidcUser;
    }
}