package com.global.exception;

import jakarta.ejb.ApplicationException;

@ApplicationException(rollback = false)
public class ShipmentNotFoundException extends SupplyChainException {

    public ShipmentNotFoundException(String trackingNumber) {
        super("SHIPMENT_NOT_FOUND", "Shipment not found with tracking number: " + trackingNumber);
    }

    public ShipmentNotFoundException(Long id) {
        super("SHIPMENT_NOT_FOUND", "Shipment not found with ID: " + id);
    }
}
