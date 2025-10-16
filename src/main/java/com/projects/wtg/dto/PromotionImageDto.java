package com.projects.wtg.dto;

import com.projects.wtg.model.PromotionImage;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PromotionImageDto {
    private Long id;
    private String s3Key;
    private Integer uploadOrder;
    private String presignedUrl; // Campo para a URL temporária

    public PromotionImageDto(PromotionImage image) {
        this.id = image.getId();
        this.s3Key = image.getS3Key();
        this.uploadOrder = image.getUploadOrder();
    }
}