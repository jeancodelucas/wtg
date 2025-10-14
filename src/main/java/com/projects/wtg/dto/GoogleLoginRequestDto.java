// .../dto/GoogleLoginRequestDto.java
package com.projects.wtg.dto;

import lombok.Data;

@Data
public class GoogleLoginRequestDto {
    private String token;
    private Double latitude;
    private Double longitude;
}