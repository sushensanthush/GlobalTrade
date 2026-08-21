package com.global.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "shipments")
@NamedQueries({
        @NamedQuery(name = "Shipment.findAll",
                query = "SELECT s FROM Shipment s ORDER BY s.createdAt DESC"),
        @NamedQuery(name = "Shipment.findByStatus",
                query = "SELECT s FROM Shipment s WHERE s.status = :status ORDER BY s.estimatedDelivery ASC"),
        @NamedQuery(name = "Shipment.findByTrackingNumber",
                query = "SELECT s FROM Shipment s WHERE s.trackingNumber = :trackingNumber"),
        @NamedQuery(name = "Shipment.findDelayed",
                query = "SELECT s FROM Shipment s WHERE s.status = com.global.entity.ShipmentStatus.IN_TRANSIT " +
                        "AND s.estimatedDelivery < :now"),
        @NamedQuery(name = "Shipment.findByOriginCountry",
                query = "SELECT s FROM Shipment s WHERE s.originCountry = :country"),
        @NamedQuery(name = "Shipment.countByStatus",
                query = "SELECT s.status, COUNT(s) FROM Shipment s GROUP BY s.status")
})
public class Shipment implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "tracking_number", unique = true, nullable = false, length = 50)
    private String trackingNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ShipmentStatus status;

    @Column(name = "origin_country", nullable = false, length = 100)
    private String originCountry;

    @Column(name = "destination_country", nullable = false, length = 100)
    private String destinationCountry;

    @Column(name = "carrier_name", length = 100)
    private String carrierName;

    @Column(name = "weight_kg", precision = 10, scale = 3)
    private BigDecimal weightKg;

    @Column(name = "declared_value", precision = 15, scale = 2)
    private BigDecimal declaredValue;

    @Column(name = "currency_code", length = 3)
    private String currencyCode;

    @Column(name = "estimated_delivery")
    private LocalDateTime estimatedDelivery;

    @Column(name = "actual_delivery")
    private LocalDateTime actualDelivery;

    @Column(name = "customs_cleared")
    private boolean customsCleared = false;

    @Column(name = "customs_declaration_number", length = 100)
    private String customsDeclarationNumber;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id")
    private Vendor vendor;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = ShipmentStatus.PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Shipment() {}


    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTrackingNumber() { return trackingNumber; }
    public void setTrackingNumber(String trackingNumber) { this.trackingNumber = trackingNumber; }
    public ShipmentStatus getStatus() { return status; }
    public void setStatus(ShipmentStatus status) { this.status = status; }
    public String getOriginCountry() { return originCountry; }
    public void setOriginCountry(String originCountry) { this.originCountry = originCountry; }
    public String getDestinationCountry() { return destinationCountry; }
    public void setDestinationCountry(String destinationCountry) { this.destinationCountry = destinationCountry; }
    public String getCarrierName() { return carrierName; }
    public void setCarrierName(String carrierName) { this.carrierName = carrierName; }
    public BigDecimal getWeightKg() { return weightKg; }
    public void setWeightKg(BigDecimal weightKg) { this.weightKg = weightKg; }
    public BigDecimal getDeclaredValue() { return declaredValue; }
    public void setDeclaredValue(BigDecimal declaredValue) { this.declaredValue = declaredValue; }
    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }
    public LocalDateTime getEstimatedDelivery() { return estimatedDelivery; }
    public void setEstimatedDelivery(LocalDateTime estimatedDelivery) { this.estimatedDelivery = estimatedDelivery; }
    public LocalDateTime getActualDelivery() { return actualDelivery; }
    public void setActualDelivery(LocalDateTime actualDelivery) { this.actualDelivery = actualDelivery; }
    public boolean isCustomsCleared() { return customsCleared; }
    public void setCustomsCleared(boolean customsCleared) { this.customsCleared = customsCleared; }
    public String getCustomsDeclarationNumber() { return customsDeclarationNumber; }
    public void setCustomsDeclarationNumber(String customsDeclarationNumber) { this.customsDeclarationNumber = customsDeclarationNumber; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public Vendor getVendor() { return vendor; }
    public void setVendor(Vendor vendor) { this.vendor = vendor; }
}
