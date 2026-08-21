package com.global.session.shipment;

import com.global.entity.Shipment;
import com.global.entity.ShipmentStatus;
import com.global.entity.Vendor;
import com.global.exception.CustomsComplianceException;
import com.global.exception.ShipmentNotFoundException;
import com.global.exception.SupplyChainException;
import com.global.interceptor.Audited;
import com.global.interceptor.PerformanceMonitored;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;


@Stateless
@Audited
@PerformanceMonitored
public class ShipmentManagementBean {

    private static final Logger LOGGER = Logger.getLogger(ShipmentManagementBean.class.getName());

    @PersistenceContext(unitName = "globalTradePU")
    private EntityManager em;


    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public Shipment createShipment(String originCountry, String destinationCountry,
                                   String carrierName, BigDecimal weightKg,
                                   BigDecimal declaredValue, String currencyCode,
                                   LocalDateTime estimatedDelivery, Long vendorId,
                                   String description) throws SupplyChainException {
        validateShipmentInputs(originCountry, destinationCountry, declaredValue);

        Shipment shipment = new Shipment();
        shipment.setTrackingNumber(generateTrackingNumber());
        shipment.setOriginCountry(originCountry);
        shipment.setDestinationCountry(destinationCountry);
        shipment.setCarrierName(carrierName);
        shipment.setWeightKg(weightKg);
        shipment.setDeclaredValue(declaredValue);
        shipment.setCurrencyCode(currencyCode != null ? currencyCode : "USD");
        shipment.setEstimatedDelivery(estimatedDelivery);
        shipment.setDescription(description);
        shipment.setStatus(ShipmentStatus.PENDING);

        if (vendorId != null) {
            Vendor vendor = em.find(Vendor.class, vendorId);
            if (vendor != null) {
                shipment.setVendor(vendor);
            }
        }

        em.persist(shipment);
        LOGGER.log(Level.INFO, "[SHIPMENT] Created shipment {0} from {1} to {2}",
                new Object[]{shipment.getTrackingNumber(), originCountry, destinationCountry});
        return shipment;
    }


    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public Shipment updateStatus(Long shipmentId, ShipmentStatus newStatus)
            throws ShipmentNotFoundException {
        Shipment shipment = em.find(Shipment.class, shipmentId);
        if (shipment == null) {
            throw new ShipmentNotFoundException(shipmentId);
        }

        ShipmentStatus previousStatus = shipment.getStatus();
        shipment.setStatus(newStatus);

        if (newStatus == ShipmentStatus.DELIVERED) {
            shipment.setActualDelivery(LocalDateTime.now());
        }

        em.merge(shipment);
        LOGGER.log(Level.INFO, "[SHIPMENT] Status updated: {0} — {1} -> {2}",
                new Object[]{shipment.getTrackingNumber(), previousStatus, newStatus});
        return shipment;
    }


    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public Shipment processCustomsClearance(Long shipmentId, String declarationNumber)
            throws ShipmentNotFoundException, CustomsComplianceException {
        Shipment shipment = em.find(Shipment.class, shipmentId);
        if (shipment == null) {
            throw new ShipmentNotFoundException(shipmentId);
        }

        if (shipment.getDeclaredValue() == null ||
                shipment.getDeclaredValue().compareTo(BigDecimal.ZERO) <= 0) {
            throw new CustomsComplianceException(shipment.getTrackingNumber(),
                    "Declared value must be greater than zero for customs clearance");
        }

        if (declarationNumber == null || declarationNumber.isBlank()) {
            throw new CustomsComplianceException(shipment.getTrackingNumber(),
                    "Customs declaration number is required");
        }

        shipment.setCustomsCleared(true);
        shipment.setCustomsDeclarationNumber(declarationNumber.trim());
        shipment.setStatus(ShipmentStatus.CUSTOMS_CLEARED);
        em.merge(shipment);

        LOGGER.log(Level.INFO, "[SHIPMENT] Customs cleared for {0}, declaration: {1}",
                new Object[]{shipment.getTrackingNumber(), declarationNumber});
        return shipment;
    }


    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public Shipment cancelShipment(Long shipmentId, String reason)
            throws ShipmentNotFoundException {
        Shipment shipment = em.find(Shipment.class, shipmentId);
        if (shipment == null) {
            throw new ShipmentNotFoundException(shipmentId);
        }

        shipment.setStatus(ShipmentStatus.CANCELLED);
        shipment.setDescription(shipment.getDescription() != null
                ? shipment.getDescription() + " | CANCELLED: " + reason
                : "CANCELLED: " + reason);
        em.merge(shipment);

        LOGGER.log(Level.WARNING, "[SHIPMENT] Shipment {0} cancelled. Reason: {1}",
                new Object[]{shipment.getTrackingNumber(), reason});
        return shipment;
    }


    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public Shipment findByTrackingNumber(String trackingNumber) throws ShipmentNotFoundException {
        try {
            return em.createNamedQuery("Shipment.findByTrackingNumber", Shipment.class)
                    .setParameter("trackingNumber", trackingNumber)
                    .getSingleResult();
        } catch (jakarta.persistence.NoResultException e) {
            throw new ShipmentNotFoundException(trackingNumber);
        }
    }


    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<Shipment> findAll() {
        return em.createNamedQuery("Shipment.findAll", Shipment.class).getResultList();
    }


    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<Shipment> findByStatus(ShipmentStatus status) {
        return em.createNamedQuery("Shipment.findByStatus", Shipment.class)
                .setParameter("status", status)
                .getResultList();
    }


    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public Shipment findById(Long id) throws ShipmentNotFoundException {
        Shipment shipment = em.find(Shipment.class, id);
        if (shipment == null) {
            throw new ShipmentNotFoundException(id);
        }
        return shipment;
    }

    private String generateTrackingNumber() {
        return "GT-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
    }

    private void validateShipmentInputs(String originCountry, String destinationCountry,
                                         BigDecimal declaredValue) throws SupplyChainException {
        if (originCountry == null || originCountry.isBlank()) {
            throw new SupplyChainException("INVALID_INPUT", "Origin country is required");
        }
        if (destinationCountry == null || destinationCountry.isBlank()) {
            throw new SupplyChainException("INVALID_INPUT", "Destination country is required");
        }
        if (declaredValue != null && declaredValue.compareTo(BigDecimal.ZERO) < 0) {
            throw new SupplyChainException("INVALID_INPUT", "Declared value cannot be negative");
        }
    }
}
