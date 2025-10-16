package com.projects.wtg.repository;

import com.projects.wtg.model.PromotionImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PromotionImageRepository extends JpaRepository<PromotionImage, Long> {
}