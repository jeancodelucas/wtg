package com.projects.wtg.repository;

import com.projects.wtg.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByEmail(String email);

    Optional<Account> findByToken(String token);

    @Query("SELECT a FROM Account a JOIN FETCH a.user u LEFT JOIN FETCH u.userPlans WHERE a.email = :email")
    Optional<Account> findByEmailWithUserAndPlans(@Param("email") String email);

}
