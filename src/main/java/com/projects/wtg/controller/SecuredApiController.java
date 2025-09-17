package com.projects.wtg.controller;

import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
@Service
@RestController
@RequestMapping("/api/secured")
public class SecuredApiController {
    @GetMapping("/user-info")
    public Principal getUserInfo(Principal principal) {
        return principal; // Retorna informações do usuário autenticado
    }
}
