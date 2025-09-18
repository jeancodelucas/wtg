package com.projects.wtg.repository;

import com.projects.wtg.model.Account;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

    // Consulta padrão
    Optional<Account> findByEmail(String email);

    // Consulta OTIMIZADA que busca a Account e o User associado em uma única query
    @EntityGraph(attributePaths = "user")
    Optional<Account> findByEmailWithUser(String email);

    Optional<Account> findByUserName(String username);
}