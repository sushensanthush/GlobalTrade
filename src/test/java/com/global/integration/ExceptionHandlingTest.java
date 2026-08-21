package com.global.integration;

import com.global.exception.*;
import jakarta.ejb.ApplicationException;
import org.junit.jupiter.api.*;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Supply Chain Exception Handling Tests")
class ExceptionHandlingTest {

    @Test
    @DisplayName("SupplyChainException — should carry @ApplicationException with rollback=true")
    void supplyChainException_hasRollbackTrue() {
        ApplicationException annotation = SupplyChainException.class
                .getAnnotation(ApplicationException.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.rollback()).isTrue();
    }

    @Test
    @DisplayName("ShipmentNotFoundException — should have rollback=false (read-only failure)")
    void shipmentNotFoundException_hasRollbackFalse() {
        ApplicationException annotation = ShipmentNotFoundException.class
                .getAnnotation(ApplicationException.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.rollback()).isFalse();
    }

    @Test
    @DisplayName("UnauthorizedAccessException — should have rollback=false (auth check is read-only)")
    void unauthorizedAccessException_hasRollbackFalse() {
        ApplicationException annotation = UnauthorizedAccessException.class
                .getAnnotation(ApplicationException.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.rollback()).isFalse();
    }

    @Test
    @DisplayName("InsufficientInventoryException — should have rollback=true (data integrity critical)")
    void insufficientInventoryException_hasRollbackTrue() {
        ApplicationException annotation = InsufficientInventoryException.class
                .getAnnotation(ApplicationException.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.rollback()).isTrue();
    }

    @Test
    @DisplayName("CustomsComplianceException — should have rollback=true (compliance critical)")
    void customsComplianceException_hasRollbackTrue() {
        ApplicationException annotation = CustomsComplianceException.class
                .getAnnotation(ApplicationException.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.rollback()).isTrue();
    }

    @Test
    @DisplayName("VendorValidationException — should have rollback=true (prevents bad vendor data)")
    void vendorValidationException_hasRollbackTrue() {
        ApplicationException annotation = VendorValidationException.class
                .getAnnotation(ApplicationException.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.rollback()).isTrue();
    }

    @Test
    @DisplayName("All exceptions — should be subclasses of SupplyChainException")
    void allExceptions_extendSupplyChainException() {
        assertThat(ShipmentNotFoundException.class).hasSuperclass(SupplyChainException.class);
        assertThat(InsufficientInventoryException.class).hasSuperclass(SupplyChainException.class);
        assertThat(CustomsComplianceException.class).hasSuperclass(SupplyChainException.class);
        assertThat(UnauthorizedAccessException.class).hasSuperclass(SupplyChainException.class);
        assertThat(VendorValidationException.class).hasSuperclass(SupplyChainException.class);
    }

    @Test
    @DisplayName("ShipmentNotFoundException — tracking number constructor formats message correctly")
    void shipmentNotFoundException_trackingNumberConstructor_formatsMessage() {
        ShipmentNotFoundException ex = new ShipmentNotFoundException("GT-DEMO00001");

        assertThat(ex.getMessage()).contains("GT-DEMO00001");
        assertThat(ex.getErrorCode()).isEqualTo("SHIPMENT_NOT_FOUND");
    }

    @Test
    @DisplayName("ShipmentNotFoundException — ID constructor formats message correctly")
    void shipmentNotFoundException_idConstructor_formatsMessage() {
        ShipmentNotFoundException ex = new ShipmentNotFoundException(42L);

        assertThat(ex.getMessage()).contains("42");
        assertThat(ex.getErrorCode()).isEqualTo("SHIPMENT_NOT_FOUND");
    }

    @Test
    @DisplayName("InsufficientInventoryException — carries itemCode, requested, and available fields")
    void insufficientInventoryException_carriesDetailFields() {
        InsufficientInventoryException ex =
                new InsufficientInventoryException("ELC-001", 500, 80);

        assertThat(ex.getItemCode()).isEqualTo("ELC-001");
        assertThat(ex.getRequested()).isEqualTo(500);
        assertThat(ex.getAvailable()).isEqualTo(80);
        assertThat(ex.getMessage()).contains("ELC-001");
        assertThat(ex.getMessage()).contains("500");
        assertThat(ex.getMessage()).contains("80");
        assertThat(ex.getErrorCode()).isEqualTo("INSUFFICIENT_INVENTORY");
    }

    @Test
    @DisplayName("CustomsComplianceException — two-arg constructor formats message with tracking number")
    void customsComplianceException_twoArgConstructor_includesTrackingNumber() {
        CustomsComplianceException ex =
                new CustomsComplianceException("GT-DEMO00002", "Missing HS code");

        assertThat(ex.getMessage()).contains("GT-DEMO00002");
        assertThat(ex.getMessage()).contains("Missing HS code");
        assertThat(ex.getErrorCode()).isEqualTo("CUSTOMS_COMPLIANCE_FAILURE");
    }

    @Test
    @DisplayName("UnauthorizedAccessException — message includes both username and operation")
    void unauthorizedAccessException_messageFormat() {
        UnauthorizedAccessException ex =
                new UnauthorizedAccessException("vendor_user", "access customs clearance");

        assertThat(ex.getMessage()).contains("vendor_user");
        assertThat(ex.getMessage()).contains("access customs clearance");
        assertThat(ex.getErrorCode()).isEqualTo("UNAUTHORIZED_ACCESS");
    }

    @Test
    @DisplayName("VendorValidationException — field/reason constructor formats message correctly")
    void vendorValidationException_fieldReasonConstructor() {
        VendorValidationException ex =
                new VendorValidationException("contactEmail", "invalid format");

        assertThat(ex.getMessage()).contains("contactEmail");
        assertThat(ex.getMessage()).contains("invalid format");
        assertThat(ex.getErrorCode()).isEqualTo("VENDOR_VALIDATION_FAILURE");
    }

    @Test
    @DisplayName("SupplyChainException — supports cause chaining for root cause preservation")
    void supplyChainException_supportsCauseChaining() {
        RuntimeException root = new RuntimeException("DB connection lost");
        SupplyChainException ex = new SupplyChainException(
                "DB_ERROR", "Supply chain operation failed", root);

        assertThat(ex.getCause()).isSameAs(root);
        assertThat(ex.getErrorCode()).isEqualTo("DB_ERROR");
    }
}
