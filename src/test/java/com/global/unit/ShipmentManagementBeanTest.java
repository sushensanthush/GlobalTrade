package com.global.unit;

import com.global.entity.Shipment;
import com.global.entity.ShipmentStatus;
import com.global.exception.CustomsComplianceException;
import com.global.exception.ShipmentNotFoundException;
import com.global.exception.SupplyChainException;
import com.global.session.shipment.ShipmentManagementBean;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
@DisplayName("ShipmentManagementBean Unit Tests")
class ShipmentManagementBeanTest {

    @Mock
    private EntityManager em;

    @InjectMocks
    private ShipmentManagementBean shipmentBean;

    private Shipment sampleShipment;

    @BeforeEach
    void setUp() {
        sampleShipment = new Shipment();
        sampleShipment.setTrackingNumber("GT-TEST00001");
        sampleShipment.setOriginCountry("China");
        sampleShipment.setDestinationCountry("United States");
        sampleShipment.setCarrierName("TestCarrier");
        sampleShipment.setDeclaredValue(BigDecimal.valueOf(5000.00));
        sampleShipment.setCurrencyCode("USD");
        sampleShipment.setStatus(ShipmentStatus.PENDING);
        sampleShipment.setWeightKg(BigDecimal.valueOf(150.0));
        sampleShipment.setEstimatedDelivery(LocalDateTime.now().plusDays(10));
    }



    @Test
    @DisplayName("createShipment — valid inputs should persist and return shipment with tracking number")
    void createShipment_validInputs_persistsShipment() throws SupplyChainException {
        doNothing().when(em).persist(any(Shipment.class));

        Shipment result = shipmentBean.createShipment(
                "China", "United States", "GlobalShip",
                BigDecimal.valueOf(200.0), BigDecimal.valueOf(8000.00), "USD",
                LocalDateTime.now().plusDays(14), null, "Test shipment");

        assertThat(result).isNotNull();
        assertThat(result.getTrackingNumber()).startsWith("GT-");
        assertThat(result.getStatus()).isEqualTo(ShipmentStatus.PENDING);
        assertThat(result.getOriginCountry()).isEqualTo("China");
        verify(em, times(1)).persist(any(Shipment.class));
    }

    @Test
    @DisplayName("createShipment — blank originCountry should throw SupplyChainException")
    void createShipment_blankOriginCountry_throwsException() {
        assertThatThrownBy(() -> shipmentBean.createShipment(
                "", "United States", "Carrier",
                BigDecimal.valueOf(100.0), BigDecimal.valueOf(1000.00), "USD",
                LocalDateTime.now().plusDays(7), null, "Desc"))
                .isInstanceOf(SupplyChainException.class)
                .hasMessageContaining("Origin country is required");

        verify(em, never()).persist(any());
    }

    @Test
    @DisplayName("createShipment — null destinationCountry should throw SupplyChainException")
    void createShipment_nullDestination_throwsException() {
        assertThatThrownBy(() -> shipmentBean.createShipment(
                "Germany", null, "Carrier",
                null, BigDecimal.valueOf(500.00), "EUR",
                LocalDateTime.now().plusDays(5), null, null))
                .isInstanceOf(SupplyChainException.class)
                .hasMessageContaining("Destination country is required");
    }

    @Test
    @DisplayName("createShipment — negative declaredValue should throw SupplyChainException")
    void createShipment_negativeValue_throwsException() {
        assertThatThrownBy(() -> shipmentBean.createShipment(
                "Japan", "Germany", "Carrier",
                BigDecimal.valueOf(50.0), BigDecimal.valueOf(-100.00), "JPY",
                LocalDateTime.now().plusDays(7), null, null))
                .isInstanceOf(SupplyChainException.class)
                .hasMessageContaining("cannot be negative");
    }


    @Test
    @DisplayName("updateStatus — valid shipment should update status and return merged entity")
    void updateStatus_validShipment_updatesStatus() throws ShipmentNotFoundException {
        when(em.find(Shipment.class, 1L)).thenReturn(sampleShipment);
        when(em.merge(any(Shipment.class))).thenReturn(sampleShipment);

        Shipment result = shipmentBean.updateStatus(1L, ShipmentStatus.IN_TRANSIT);

        assertThat(result.getStatus()).isEqualTo(ShipmentStatus.IN_TRANSIT);
        verify(em).merge(sampleShipment);
    }

    @Test
    @DisplayName("updateStatus — DELIVERED status should set actualDelivery timestamp")
    void updateStatus_deliveredStatus_setsActualDelivery() throws ShipmentNotFoundException {
        when(em.find(Shipment.class, 1L)).thenReturn(sampleShipment);
        when(em.merge(any(Shipment.class))).thenReturn(sampleShipment);

        Shipment result = shipmentBean.updateStatus(1L, ShipmentStatus.DELIVERED);

        assertThat(result.getActualDelivery()).isNotNull();
        assertThat(result.getActualDelivery()).isBeforeOrEqualTo(LocalDateTime.now().plusSeconds(1));
    }

    @Test
    @DisplayName("updateStatus — unknown shipment ID should throw ShipmentNotFoundException")
    void updateStatus_unknownId_throwsShipmentNotFoundException() {
        when(em.find(Shipment.class, 999L)).thenReturn(null);

        assertThatThrownBy(() -> shipmentBean.updateStatus(999L, ShipmentStatus.IN_TRANSIT))
                .isInstanceOf(ShipmentNotFoundException.class)
                .hasMessageContaining("999");

        verify(em, never()).merge(any());
    }


    @Test
    @DisplayName("processCustomsClearance — valid declaration should mark shipment cleared")
    void processCustomsClearance_validDeclaration_marksCleared()
            throws ShipmentNotFoundException, CustomsComplianceException {
        when(em.find(Shipment.class, 1L)).thenReturn(sampleShipment);
        when(em.merge(any(Shipment.class))).thenReturn(sampleShipment);

        Shipment result = shipmentBean.processCustomsClearance(1L, "CUST-2026-00123");

        assertThat(result.isCustomsCleared()).isTrue();
        assertThat(result.getCustomsDeclarationNumber()).isEqualTo("CUST-2026-00123");
        assertThat(result.getStatus()).isEqualTo(ShipmentStatus.CUSTOMS_CLEARED);
    }

    @Test
    @DisplayName("processCustomsClearance — blank declarationNumber should throw CustomsComplianceException")
    void processCustomsClearance_blankDeclaration_throwsException() {
        when(em.find(Shipment.class, 1L)).thenReturn(sampleShipment);

        assertThatThrownBy(() -> shipmentBean.processCustomsClearance(1L, "  "))
                .isInstanceOf(CustomsComplianceException.class)
                .hasMessageContaining("declaration number");

        verify(em, never()).merge(any());
    }

    @Test
    @DisplayName("processCustomsClearance — zero declaredValue should throw CustomsComplianceException")
    void processCustomsClearance_zeroDeclaredValue_throwsException() {
        sampleShipment.setDeclaredValue(BigDecimal.ZERO);
        when(em.find(Shipment.class, 1L)).thenReturn(sampleShipment);

        assertThatThrownBy(() -> shipmentBean.processCustomsClearance(1L, "CUST-2026-99999"))
                .isInstanceOf(CustomsComplianceException.class)
                .hasMessageContaining("greater than zero");
    }


    @Test
    @DisplayName("cancelShipment — should mark shipment as CANCELLED and append reason")
    void cancelShipment_existingShipment_cancelsSuccessfully() throws ShipmentNotFoundException {
        when(em.find(Shipment.class, 1L)).thenReturn(sampleShipment);
        when(em.merge(any(Shipment.class))).thenReturn(sampleShipment);

        Shipment result = shipmentBean.cancelShipment(1L, "Customer request");

        assertThat(result.getStatus()).isEqualTo(ShipmentStatus.CANCELLED);
        assertThat(result.getDescription()).contains("CANCELLED");
        assertThat(result.getDescription()).contains("Customer request");
    }

    @Test
    @DisplayName("cancelShipment — unknown ID should throw ShipmentNotFoundException")
    void cancelShipment_unknownId_throwsNotFoundException() {
        when(em.find(Shipment.class, 42L)).thenReturn(null);

        assertThatThrownBy(() -> shipmentBean.cancelShipment(42L, "Reason"))
                .isInstanceOf(ShipmentNotFoundException.class);
    }



    @Test
    @DisplayName("findAll — should return list of shipments from EntityManager query")
    void findAll_returnsShipmentList() {
        @SuppressWarnings("unchecked")
        TypedQuery<Shipment> mockQuery = mock(TypedQuery.class);
        when(em.createNamedQuery("Shipment.findAll", Shipment.class)).thenReturn(mockQuery);
        when(mockQuery.getResultList()).thenReturn(List.of(sampleShipment));

        List<Shipment> result = shipmentBean.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTrackingNumber()).isEqualTo("GT-TEST00001");
    }

    @Test
    @DisplayName("findByStatus — should delegate to named query with correct status parameter")
    void findByStatus_delegatesToNamedQuery() {
        @SuppressWarnings("unchecked")
        TypedQuery<Shipment> mockQuery = mock(TypedQuery.class);
        when(em.createNamedQuery("Shipment.findByStatus", Shipment.class)).thenReturn(mockQuery);
        when(mockQuery.setParameter(eq("status"), eq(ShipmentStatus.IN_TRANSIT))).thenReturn(mockQuery);
        when(mockQuery.getResultList()).thenReturn(List.of(sampleShipment));

        List<Shipment> result = shipmentBean.findByStatus(ShipmentStatus.IN_TRANSIT);

        assertThat(result).isNotEmpty();
        verify(mockQuery).setParameter("status", ShipmentStatus.IN_TRANSIT);
    }
}
