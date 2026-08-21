package com.global.exception;

import jakarta.ejb.ApplicationException;

@ApplicationException(rollback = false)
public class UnauthorizedAccessException extends SupplyChainException {

    public UnauthorizedAccessException(String username, String operation) {
        super("UNAUTHORIZED_ACCESS",
                String.format("User '%s' is not authorized to perform operation: %s", username, operation));
    }
}
