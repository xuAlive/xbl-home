package com.xu.home.utils.common.enums;

/**
 * Token异常类（预留扩展）
 */
public class TokenException extends RuntimeException {

    public TokenException() {
        super();
    }

    public TokenException(String message) {
        super(message);
    }

    public TokenException(String message, Throwable cause) {
        super(message, cause);
    }
}
