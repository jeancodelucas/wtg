package com.projects.wtg.service;

import com.projects.wtg.model.Account;
import com.projects.wtg.model.Promotion;
import com.projects.wtg.model.User;
import com.projects.wtg.repository.AccountRepository;
import com.projects.wtg.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    private UserService (UserRepository userRepository, AccountRepository accountRepository){
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
    }

    public User createUserWithAccount(User user, Account account){
        user.setAccount(account);
        account.setUser(user);
        return userRepository.save(user);
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
