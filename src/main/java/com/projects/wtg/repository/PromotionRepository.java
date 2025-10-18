package com.projects.wtg.repository;

import com.projects.wtg.model.Promotion;
import com.projects.wtg.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.locationtech.jts.geom.Point;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

/**
 * Repositório para a entidade Promotion.
 * Estende JpaSpecificationExecutor para permitir a criação de queries dinâmicas.
 */
public interface PromotionRepository extends JpaRepository<Promotion, Long>, JpaSpecificationExecutor<Promotion> {

    /**
     * Busca uma promoção pelo seu ID e pelo usuário associado.
     * @param id O ID da promoção.
     * @param user O usuário dono da promoção.
     * @return um Optional contendo a promoção, se encontrada.
     */
    Optional<Promotion> findByIdAndUser(Long id, User user);

    /**
     * Busca os IDs das promoções que estão dentro de um raio específico a partir de uma localização.
     * Esta é uma query nativa otimizada para performance, retornando apenas os IDs.
     * @param userLocation A localização do usuário como um objeto Point.
     * @param radiusInMeters O raio da busca em metros.
     * @return Uma lista de IDs (Long) das promoções encontradas.
     */
    @Query(value = "SELECT p.id FROM appwtg.promotion p WHERE ST_DWithin(p.point, :userLocation, :radiusInMeters)", nativeQuery = true)
    List<Long> findIdsWithinRadius(
            @Param("userLocation") Point userLocation,
            @Param("radiusInMeters") double radiusInMeters
    );

    Optional<Promotion> findByUser(User user);
}
