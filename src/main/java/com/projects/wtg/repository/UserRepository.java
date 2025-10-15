// src/main/java/com/projects/wtg/repository/UserRepository.java

package com.projects.wtg.repository;

import com.projects.wtg.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Busca um usuário (User) com base no e-mail associado à sua conta (Account).
     * Esta query customizada faz a junção (JOIN) entre a tabela User e Account.
     *
     * @param email O e-mail da conta do usuário a ser buscado.
     * @return um Optional contendo o User, se encontrado.
     */
    @Query("SELECT u FROM User u WHERE u.account.email = :email")
    Optional<User> findByAccountEmail(@Param("email") String email);

}