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

import java.util.Optional;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;

    public CustomOAuth2UserService(UserRepository userRepository, AccountRepository accountRepository) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
    }

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        System.out.println(">>> Método loadUser iniciado. Buscando informações do usuário..."); // Log de início
        OAuth2User oAuth2User = super.loadUser(userRequest);

        try {
            String email = oAuth2User.getAttribute("email");
            String given_name = oAuth2User.getAttribute("given_name");
            String family_name = oAuth2User.getAttribute("family_name");
            String picture = oAuth2User.getAttribute("picture");
            String provider = userRequest.getClientRegistration().getRegistrationId();
            String sub = oAuth2User.getAttribute("sub");

            System.out.println(">>> Email do usuário: " + email); // Log de informações

            Optional<Account> existingAccount = accountRepository.findByEmail(email);
            Account account;
            User user;

            if (existingAccount.isPresent()) {
                System.out.println(">>> Usuário existente encontrado. Atualizando dados..."); // Log de atualização
                account = existingAccount.get();
                user = account.getUser();

                user.setFirstName(given_name);
                user.setFullName(family_name);
                user.setPictureUrl(picture);

                account.setUserName(email);
                account.setEmail(email);
                account.setLoginSub(sub);
                account.setLoginProvider(provider);
                account.setLocale(oAuth2User.getAttribute("locale"));
                if (oAuth2User.getAttribute("email_verified") != null) {
                    account.setEmailVerified(oAuth2User.getAttribute("email_verified"));
                }

                userRepository.save(user);
                System.out.println(">>> Usuário atualizado com sucesso!");

            } else {
                System.out.println(">>> Nenhum usuário encontrado. Criando novo usuário..."); // Log de criação
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
                System.out.println(">>> Novo usuário criado e salvo com sucesso!");
            }

        } catch (Exception e) {
            System.err.println(">>> Erro fatal ao salvar usuário: " + e.getMessage()); // Captura de erros
            e.printStackTrace();
        }

        return oAuth2User;
    }
}