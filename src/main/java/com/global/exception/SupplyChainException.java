package com.global.exception;

import jakarta.ejb.ApplicationException;

@ApplicationException(rollback = true)
public class SupplyChainException extends Exception {

    private final String errorCode;

    public SupplyChainException(String message) {
        super(message);
        this.errorCode = "SCM_GENERIC_ERROR";
    }

    public SupplyChainException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public SupplyChainException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
