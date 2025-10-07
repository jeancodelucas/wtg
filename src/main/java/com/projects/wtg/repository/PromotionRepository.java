package com.projects.wtg.repository;

import com.projects.wtg.model.Promotion;
import com.projects.wtg.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import org.locationtech.jts.geom.Point;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;


public interface PromotionRepository extends JpaRepository<Promotion, Long> {
    Optional<Promotion> findByIdAndUser(Long id, User user);

    @Query(value = "SELECT * FROM appwtg.promotion p WHERE ST_DWithin(p.point, :userLocation, :radiusInMeters)", nativeQuery = true)
    List<Promotion> findPromotionsWithinRadius(
            @Param("userLocation") Point userLocation,
            @Param("radiusInMeters") double radiusInMeters
    );
}