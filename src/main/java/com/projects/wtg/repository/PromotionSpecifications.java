package com.projects.wtg.repository;

import com.projects.wtg.model.Promotion;
import com.projects.wtg.model.PromotionType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import java.util.List;

public class PromotionSpecifications {

    public static Specification<Promotion> createSpecification(PromotionType promotionType, List<Long> idsInRadius) {
        return (root, query, criteriaBuilder) -> {
            Predicate predicate = criteriaBuilder.conjunction();

            // Adiciona filtro por tipo, se fornecido
            if (promotionType != null) {
                predicate = criteriaBuilder.and(predicate,
                        criteriaBuilder.equal(root.get("promotionType"), promotionType));
            }

            // Adiciona filtro por IDs de geolocalização, se fornecidos
            if (idsInRadius != null && !idsInRadius.isEmpty()) {
                predicate = criteriaBuilder.and(predicate, root.get("id").in(idsInRadius));
            }

            return predicate;
        };
    }
}
