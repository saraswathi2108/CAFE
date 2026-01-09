package com.anasol.cafe.exceptions;

public class CartProcessingException extends RuntimeException {
    public CartProcessingException(String message) {
        super(message);
    }
}
