package com.projects.wtg.service;

import com.projects.wtg.model.Account;
import com.projects.wtg.repository.AccountRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class JpaUserDetailsService implements UserDetailsService {

    private final AccountRepository accountRepository;

    public JpaUserDetailsService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado com o e-mail: " + email));

        // A lógica de reativação foi movida para o AuthController.
        // O Spring Security sempre retornará um UserDetails "habilitado" para que a senha possa ser validada.
        return new User(
                account.getEmail(),
                account.getPassword(),
                true, // enabled
                true, // accountNonExpired
                true, // credentialsNonExpired
                true, // accountNonLocked
                Collections.emptyList()
        );
    }
}