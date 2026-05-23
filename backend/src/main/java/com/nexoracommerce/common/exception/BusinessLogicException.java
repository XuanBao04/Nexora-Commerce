package com.nexoracommerce.common.exception;

import org.springframework.http.HttpStatus;

public class BusinessLogicException extends BusinessException {
    public BusinessLogicException(String message) {
        super(message, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    public BusinessLogicException(String message, Throwable cause) {
        super(message, cause, HttpStatus.UNPROCESSABLE_ENTITY);
    }
}
