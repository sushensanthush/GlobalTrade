package com.global.exception;

import jakarta.ejb.ApplicationException;

@ApplicationException(rollback = true)
public class InsufficientInventoryException extends SupplyChainException {

    private final String itemCode;
    private final int requested;
    private final int available;

    public InsufficientInventoryException(String itemCode, int requested, int available) {
        super("INSUFFICIENT_INVENTORY",
                String.format("Insufficient inventory for item %s: requested %d, available %d",
                        itemCode, requested, available));
        this.itemCode = itemCode;
        this.requested = requested;
        this.available = available;
    }

    public String getItemCode() { return itemCode; }
    public int getRequested() { return requested; }
    public int getAvailable() { return available; }
}
