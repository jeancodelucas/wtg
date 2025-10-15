// src/main/java/com/projects/wtg/controller/CommentController.java

package com.projects.wtg.controller;

import com.projects.wtg.dto.CommentDto;
import com.projects.wtg.dto.CreateCommentRequestDto;
import com.projects.wtg.service.CommentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/comments")
public class CommentController {

    @Autowired
    private CommentService commentService;

    /**
     * Endpoint para criar um novo comentário em uma promoção.
     * O usuário precisa estar autenticado.
     */
    @PostMapping
    public ResponseEntity<CommentDto> createComment(@Valid @RequestBody CreateCommentRequestDto request) {
        CommentDto createdComment = commentService.createComment(request);
        return new ResponseEntity<>(createdComment, HttpStatus.CREATED);
    }

    /**
     * Endpoint para excluir um comentário.
     * Apenas o autor do comentário ou um administrador podem excluí-lo.
     */
    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long commentId) {
        commentService.deleteComment(commentId);
        return ResponseEntity.noContent().build(); // Retorna 204 No Content, indicando sucesso na exclusão
    }
}