package com.projects.wtg.service;

import com.projects.wtg.model.Account;
import com.projects.wtg.model.User;
import com.projects.wtg.repository.AccountRepository;
import com.projects.wtg.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    // Injeção de dependência via construtor
    public AccountService(AccountRepository accountRepository, UserRepository userRepository) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
    }

    public Account createAccountForUsers(Account newAccount, Long userId) {
        User user  = userRepository.findById(userId).orElseThrow(()-> new RuntimeException("Usuário não encontrado"));

        Account savedAccount = accountRepository.save(newAccount);
        user.setAccount(savedAccount);
        userRepository.save(user);

        return savedAccount;
    }

    // Atualizar conta já existente de um usuário
    public Account updateAccountForUser(Long userId, Account accountData) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        Account account = user.getAccount();
        if (account == null) {
            throw new RuntimeException("Usuário não possui conta associada");
        }

        // atualiza os campos
        account.setUserName(accountData.getUserName());
        account.setEmail(accountData.getEmail());
        account.setSecondEmail(accountData.getSecondEmail());
        account.setPassword(accountData.getPassword());
        account.setConfirmPassword(accountData.getConfirmPassword());

        return accountRepository.save(account);
    }

}
