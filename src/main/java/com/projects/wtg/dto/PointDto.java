// src/main/java/com/projects/wtg/dto/PointDto.java

package com.projects.wtg.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class PointDto {
    private Long userId;
    private Double latitude;
    private Double longitude;
    private LocalDateTime timestamp;
}