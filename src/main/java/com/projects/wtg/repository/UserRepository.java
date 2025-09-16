package com.projects.wtg.repository;

import com.projects.wtg.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByFirstName(String firstName);
}
