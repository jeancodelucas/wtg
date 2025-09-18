package com.projects.wtg.repository;

import com.projects.wtg.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

    /**
     * Busca uma conta pelo e-mail.
     * Esta é a consulta padrão, que pode carregar o usuário associado de forma "preguiçosa" (lazy).
     */
    Optional<Account> findByEmail(String email);

    /**
     * Busca uma conta pelo e-mail e também carrega ("fetches") o usuário associado
     * na mesma consulta SQL, evitando o problema de N+1 e melhorando a performance.
     * Esta é a versão otimizada.
     */
    @Query("SELECT a FROM Account a JOIN FETCH a.user WHERE a.email = :email")
    Optional<Account> findByEmailWithUser(@Param("email") String email);

    Optional<Account> findByUserName(String username);
}