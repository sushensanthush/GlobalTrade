package com.global.exception;

import jakarta.ejb.ApplicationException;

@ApplicationException(rollback = true)
public class VendorValidationException extends SupplyChainException {

    public VendorValidationException(String message) {
        super("VENDOR_VALIDATION_FAILURE", message);
    }

    public VendorValidationException(String field, String reason) {
        super("VENDOR_VALIDATION_FAILURE",
                String.format("Vendor validation failed for field '%s': %s", field, reason));
    }
}
