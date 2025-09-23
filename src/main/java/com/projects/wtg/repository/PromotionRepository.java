package com.projects.wtg.repository;

import com.projects.wtg.model.Promotion;
import com.projects.wtg.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PromotionRepository extends JpaRepository<Promotion, Long> {
    Optional<Promotion> findByIdAndUser(Long id, User user);
}