package com.projects.wtg.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponseDto {
    private String status;
    private String messagem; // Mantendo 'messagem' como solicitado
    private int code;
    private UserDto user;
}