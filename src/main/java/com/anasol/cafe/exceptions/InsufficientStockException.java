package com.anasol.cafe.exceptions;

public class InsufficientStockException extends ValidationException {
    public InsufficientStockException(String message) {
        super(message);
    }
    
    public InsufficientStockException(String productName, Double available, Double requested) {
        super(String.format("Insufficient stock for %s. Available: %.2f, Requested: %.2f", 
                productName, available, requested));
    }
    
    public InsufficientStockException(String productName, Double available, Double existing, Double additional) {
        super(String.format("Insufficient stock for %s. Available: %.2f, Already in cart: %.2f, Additional requested: %.2f",
                productName, available, existing, additional));
    }
}