// src/main/java/com/projects/wtg/repository/CommentRepository.java

package com.projects.wtg.repository;

import com.projects.wtg.model.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    // Você pode adicionar métodos de busca customizados aqui no futuro, se precisar.
    // Ex: List<Comment> findByPromotionId(Long promotionId);
}