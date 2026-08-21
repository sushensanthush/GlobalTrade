package com.global.exception;

import jakarta.ejb.ApplicationException;

@ApplicationException(rollback = true)
public class CustomsComplianceException extends SupplyChainException {

    public CustomsComplianceException(String message) {
        super("CUSTOMS_COMPLIANCE_FAILURE", message);
    }

    public CustomsComplianceException(String trackingNumber, String reason) {
        super("CUSTOMS_COMPLIANCE_FAILURE",
                String.format("Customs compliance failure for shipment %s: %s", trackingNumber, reason));
    }
}
