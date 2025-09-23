package com.projects.wtg.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT) // HTTP 409 Conflict é ideal para este caso
public class PromotionAlreadyExistsException extends RuntimeException {
    public PromotionAlreadyExistsException(String message) {
        super(message);
    }
}