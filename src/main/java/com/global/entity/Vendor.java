package com.global.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "vendors")
@NamedQueries({
        @NamedQuery(name = "Vendor.findAll",
                query = "SELECT v FROM Vendor v ORDER BY v.name ASC"),
        @NamedQuery(name = "Vendor.findByCountry",
                query = "SELECT v FROM Vendor v WHERE v.countryCode = :countryCode"),
        @NamedQuery(name = "Vendor.findUnderperforming",
                query = "SELECT v FROM Vendor v WHERE v.performanceScore < :threshold AND v.active = true")
})
public class Vendor implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "vendor_code", unique = true, nullable = false, length = 50)
    private String vendorCode;

    @Column(name = "country_code", nullable = false, length = 10)
    private String countryCode;

    @Column(name = "contact_email", length = 200)
    private String contactEmail;

    @Column(name = "contact_phone", length = 50)
    private String contactPhone;

    @Column(name = "performance_score", precision = 5, scale = 2)
    private BigDecimal performanceScore = BigDecimal.valueOf(100.0);

    @Column(name = "on_time_delivery_rate", precision = 5, scale = 2)
    private BigDecimal onTimeDeliveryRate = BigDecimal.valueOf(100.0);

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "last_evaluated")
    private LocalDateTime lastEvaluated;

    @OneToMany(mappedBy = "vendor", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Shipment> shipments = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Vendor() {}

    public Vendor(String name, String vendorCode, String countryCode, String contactEmail) {
        this.name = name;
        this.vendorCode = vendorCode;
        this.countryCode = countryCode;
        this.contactEmail = contactEmail;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getVendorCode() { return vendorCode; }
    public void setVendorCode(String vendorCode) { this.vendorCode = vendorCode; }
    public String getCountryCode() { return countryCode; }
    public void setCountryCode(String countryCode) { this.countryCode = countryCode; }
    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }
    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }
    public BigDecimal getPerformanceScore() { return performanceScore; }
    public void setPerformanceScore(BigDecimal performanceScore) { this.performanceScore = performanceScore; }
    public BigDecimal getOnTimeDeliveryRate() { return onTimeDeliveryRate; }
    public void setOnTimeDeliveryRate(BigDecimal onTimeDeliveryRate) { this.onTimeDeliveryRate = onTimeDeliveryRate; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getLastEvaluated() { return lastEvaluated; }
    public void setLastEvaluated(LocalDateTime lastEvaluated) { this.lastEvaluated = lastEvaluated; }
    public List<Shipment> getShipments() { return shipments; }
    public void setShipments(List<Shipment> shipments) { this.shipments = shipments; }
}
