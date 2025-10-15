// src/main/java/com/projects/wtg/service/CommentService.java

package com.projects.wtg.service;

import com.projects.wtg.dto.CommentDto;
import com.projects.wtg.dto.CreateCommentRequestDto;
import com.projects.wtg.model.Comment;
import com.projects.wtg.model.Promotion;
import com.projects.wtg.model.User;
import com.projects.wtg.model.UserType;
import com.projects.wtg.repository.CommentRepository;
import com.projects.wtg.repository.PromotionRepository;
import com.projects.wtg.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommentService {

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PromotionRepository promotionRepository;

    @Transactional
    public CommentDto createComment(CreateCommentRequestDto request) {
        // Pega o email do usuário logado a partir do contexto de segurança
        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByAccountEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Usuário logado não encontrado."));

        // Busca a promoção pelo ID
        Promotion promotion = promotionRepository.findById(request.getPromotionId())
                .orElseThrow(() -> new RuntimeException("Promoção não encontrada com o ID: " + request.getPromotionId()));

        // Cria a nova entidade de comentário
        Comment newComment = Comment.builder()
                .user(currentUser)
                .promotion(promotion)
                .comment(request.getComment())
                .build();

        // Salva no banco e retorna o DTO
        Comment savedComment = commentRepository.save(newComment);
        return new CommentDto(savedComment);
    }

    @Transactional
    public void deleteComment(Long commentId) {
        // Pega o email do usuário logado
        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByAccountEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Usuário logado não encontrado."));

        // Busca o comentário a ser deletado
        Comment commentToDelete = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comentário não encontrado com o ID: " + commentId));

        // Verifica se o usuário logado é o autor do comentário OU se é um ADMIN
        boolean isAdmin = currentUser.getUserType() == UserType.ADMIN;
        boolean isAuthor = commentToDelete.getUser().getId().equals(currentUser.getId());

        if (isAuthor || isAdmin) {
            commentRepository.delete(commentToDelete);
        } else {
            // Se não tiver permissão, lança uma exceção
            throw new AccessDeniedException("Você não tem permissão para excluir este comentário.");
        }
    }
}