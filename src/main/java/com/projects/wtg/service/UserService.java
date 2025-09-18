package com.projects.wtg.service;

import com.projects.wtg.dto.UserRegistrationDto;
import com.projects.wtg.exception.EmailAlreadyExistsException;
import com.projects.wtg.model.Account;
import com.projects.wtg.model.Promotion;
import com.projects.wtg.model.User;
import com.projects.wtg.repository.AccountRepository;
import com.projects.wtg.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class UserService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private static final Pattern STRONG_PASSWORD_PATTERN =
            Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$");


    public UserService (UserRepository userRepository, AccountRepository accountRepository, PasswordEncoder passwordEncoder){
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User createUserWithAccount(UserRegistrationDto userRegistrationDto){
        // Validação: Verifique se o e-mail já existe
        accountRepository.findByEmail(userRegistrationDto.getEmail())
                .ifPresent(account -> {
                    throw new EmailAlreadyExistsException("Email já cadastrado!");
                });
        // 2. Valida a força da senha
        if (!isPasswordStrong(userRegistrationDto.getPassword())) {
            throw new IllegalArgumentException("A senha não atende aos critérios de segurança (mínimo 8 caracteres, com maiúscula, minúscula, número e caractere especial).");
        }
        // 3. Verifica se as senhas coincidem
        if (!userRegistrationDto.getPassword().equals(userRegistrationDto.getConfirmPassword())) {
            throw new IllegalArgumentException("As senhas não coincidem.");
        }

        User user = new User();
        user.setFullName(userRegistrationDto.getFullName());
        user.setFirstName(userRegistrationDto.getFirstName());
        user.setPictureUrl(userRegistrationDto.getPictureUrl());
        user.setCreatedAt(LocalDateTime.now());
        user.setCreatedAt(LocalDateTime.now());
        user.setBirthday(userRegistrationDto.getBirthday());
        user.setActive(true);

        Account account = new Account();
        account.setUserName(userRegistrationDto.getUserName());
        account.setEmail(userRegistrationDto.getEmail());

        account.setPassword(passwordEncoder.encode(userRegistrationDto.getPassword()));
        user.setAccount(account);
        return userRepository.save(user);
    }

    private boolean isPasswordStrong(String password) {
        if (password == null) {
            return false;
        }
        return STRONG_PASSWORD_PATTERN.matcher(password).matches();
    }

    public User updateUserWithAccount(Long userId, User newUserData, Account newAccountData, List<Promotion> promotions){
        return userRepository.findById(userId).map(user->{
                user.setBirthday(newUserData.getBirthday());
                user.setPhone(newUserData.getPhone());
                user.setFirstName(newUserData.getFirstName());
                user.setFullName(newUserData.getFullName());
                user.setPromotions(promotions);

                Account account = user.getAccount();
                if(account == null){
                    account = newAccountData;
                    account.setUser(user);
                    user.setAccount(account);
                }else{
                    account.setUserName(newAccountData.getUserName());
                    account.setEmail(newAccountData.getEmail());
                    account.setSecondEmail(newAccountData.getSecondEmail());
                    account.setPassword(newAccountData.getPassword());
                    account.setConfirmPassword(newAccountData.getConfirmPassword());
                }

                return userRepository.save(user);

        }).orElseThrow(()->new RuntimeException("Usuário não encontrado"));
    }
}
