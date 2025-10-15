// src/main/java/com/projects/wtg/dto/CommentDto.java

package com.projects.wtg.dto;

import com.projects.wtg.model.Comment;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CommentDto {
    private Long id;
    private String comment;
    private Long userId;
    private String userFirstName;
    private Long promotionId;
    private Integer likeCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public CommentDto(Comment comment) {
        this.id = comment.getId();
        this.comment = comment.getComment();
        this.userId = comment.getUser().getId();
        this.userFirstName = comment.getUser().getFirstName(); // Adicionando o nome do usuário
        this.promotionId = comment.getPromotion().getId();
        this.likeCount = comment.getLikeCount();
        this.createdAt = comment.getCreatedAt();
        this.updatedAt = comment.getUpdatedAt();
    }
}