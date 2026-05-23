package com.nexoracommerce.common.exception;

import org.springframework.http.HttpStatus;

public class InvalidInputException extends BusinessException {
    public InvalidInputException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }

    public InvalidInputException(String message, Throwable cause) {
        super(message, cause, HttpStatus.BAD_REQUEST);
    }
}
