// src/main/java/com/projects/wtg/dto/LoginResponseDto.java

package com.projects.wtg.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoginResponseDto {
    private String status;
    private String messagem; // Mantendo 'messagem' como solicitado
    private int code;
    private UserDto user;
    private List<PromotionDto> nearbyPromotions;
    private Boolean isRegistrationComplete;

    public LoginResponseDto(String status, String messagem, int code, UserDto user, List<PromotionDto> nearbyPromotions) {
        this.status = status;
        this.messagem = messagem;
        this.code = code;
        this.user = user;
        this.nearbyPromotions = nearbyPromotions;
    }
}