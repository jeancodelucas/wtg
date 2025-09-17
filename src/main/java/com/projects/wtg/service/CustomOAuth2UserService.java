package com.projects.wtg.service;

import com.projects.wtg.model.Account;
import com.projects.wtg.model.User;
import com.projects.wtg.repository.AccountRepository;
import com.projects.wtg.repository.UserRepository;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private static final Logger logger = LoggerFactory.getLogger(CustomOAuth2UserService.class);

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;

    public CustomOAuth2UserService(UserRepository userRepository, AccountRepository accountRepository) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
    }

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        logger.info(">>> Método loadUser iniciado. Buscando informações do usuário...");
        OAuth2User oAuth2User = super.loadUser(userRequest);

        try {
            String email = oAuth2User.getAttribute("email");
            String given_name = oAuth2User.getAttribute("given_name");
            String family_name = oAuth2User.getAttribute("family_name");
            String picture = oAuth2User.getAttribute("picture");
            String provider = userRequest.getClientRegistration().getRegistrationId();
            String sub = oAuth2User.getAttribute("sub");

            logger.info(">>> Email do usuário: {}", email);

            Optional<Account> existingAccount = accountRepository.findByEmail(email);
            Account account;
            User user;

            if (existingAccount.isPresent()) {
                logger.info(">>> Usuário existente encontrado. Atualizando dados...");
                account = existingAccount.get();
                user = account.getUser();

                if (user != null) {
                    user.setFirstName(given_name);
                    user.setFullName(family_name);
                    user.setPictureUrl(picture);
                }

                account.setUserName(email);
                account.setEmail(email);
                account.setLoginSub(sub);
                account.setLoginProvider(provider);
                account.setLocale(oAuth2User.getAttribute("locale"));
                if (oAuth2User.getAttribute("email_verified") != null) {
                    account.setEmailVerified(oAuth2User.getAttribute("email_verified"));
                }

                userRepository.save(user);
                logger.info(">>> Usuário atualizado com sucesso!");

            } else {
                logger.info(">>> Nenhum usuário encontrado. Criando novo usuário...");
                user = new User();
                user.setFirstName(given_name);
                user.setFullName(family_name);
                user.setPictureUrl(picture);

                account = new Account();
                account.setUserName(email);
                account.setEmail(email);
                account.setLoginSub(sub);
                account.setLoginProvider(provider);
                account.setLocale(oAuth2User.getAttribute("locale"));
                if (oAuth2User.getAttribute("email_verified") != null) {
                    account.setEmailVerified(oAuth2User.getAttribute("email_verified"));
                }

                user.setAccount(account);
                account.setUser(user);

                userRepository.save(user);
                logger.info(">>> Novo usuário criado e salvo com sucesso!");
            }

        } catch (Exception e) {
            logger.error(">>> Erro fatal ao salvar usuário: {}", e.getMessage(), e);
        }

        return oAuth2User;
    }
}