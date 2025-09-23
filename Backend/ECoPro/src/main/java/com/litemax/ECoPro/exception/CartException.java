package com.litemax.ECoPro.exception;

public class CartException extends RuntimeException {
    public CartException(String message) {
        super(message);
    }

    public CartException(String message, Throwable cause) {
        super(message, cause);
    }
}

class InsufficientStockException extends CartException {
    public InsufficientStockException(String message) {
        super(message);
    }
}

class CartItemNotFoundException extends CartException {
    public CartItemNotFoundException(String message) {
        super(message);
    }
}

class WishlistItemAlreadyExistsException extends CartException {
    public WishlistItemAlreadyExistsException(String message) {
        super(message);
    }
}