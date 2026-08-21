package com.global.util;

import com.global.entity.*;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.logging.Level;
import java.util.logging.Logger;


@Singleton
@Startup
public class DataInitializer {

    private static final Logger LOGGER = Logger.getLogger(DataInitializer.class.getName());

    @PersistenceContext(unitName = "globalTradePU")
    private EntityManager em;

    @PostConstruct
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void initialize() {
        LOGGER.log(Level.INFO, "[INIT] DataInitializer starting...");
        try {
            createDefaultUsers();
            createSampleVendors();
            createSampleInventory();
            createSampleShipments();
            LOGGER.log(Level.INFO, "[INIT] DataInitializer completed successfully.");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "[INIT] DataInitializer failed: " + e.getMessage(), e);
        }
    }

    private void createDefaultUsers() {
        String adminHash = hashPassword("admin123");
        String logHash = hashPassword("logistics123");
        String customsHash = hashPassword("customs123");
        String warehouseHash = hashPassword("warehouse123");
        String vendorHash = hashPassword("vendor123");

        createUserIfNotExists("admin", "admin@globaltrade.com", adminHash,
                "System Administrator", UserRole.SYSTEM_ADMIN);
        createUserIfNotExists("logistics", "logistics@globaltrade.com", logHash,
                "Logistics Coordinator", UserRole.LOGISTICS_COORDINATOR);
        createUserIfNotExists("customs", "customs@globaltrade.com", customsHash,
                "Customs Agent", UserRole.CUSTOMS_AGENT);
        createUserIfNotExists("warehouse", "warehouse@globaltrade.com", warehouseHash,
                "Warehouse Manager", UserRole.WAREHOUSE_MANAGER);
        createUserIfNotExists("vendor", "vendor@acmecorp.com", vendorHash,
                "Vendor Representative", UserRole.VENDOR_REPRESENTATIVE);
    }

    private void createUserIfNotExists(String username, String email, String passwordHash,
                                        String fullName, UserRole role) {
        long count = em.createQuery("SELECT COUNT(u) FROM User u WHERE u.username = :u", Long.class)
                .setParameter("u", username).getSingleResult();
        if (count == 0) {
            User user = new User(username, email, passwordHash, fullName, role);
            em.persist(user);
            LOGGER.log(Level.INFO, "[INIT] Created user: {0} ({1})", new Object[]{username, role});
        }
    }

    private void createSampleVendors() {
        long count = em.createQuery("SELECT COUNT(v) FROM Vendor v", Long.class).getSingleResult();
        if (count > 0) return;

        Vendor v1 = new Vendor("AcmeCorp Logistics", "VND-ACME001", "US", "contact@acmecorp.com");
        v1.setContactPhone("+1-555-0100");
        v1.setPerformanceScore(BigDecimal.valueOf(92.5));
        v1.setOnTimeDeliveryRate(BigDecimal.valueOf(94.1));
        em.persist(v1);

        Vendor v2 = new Vendor("SinoFreight Ltd", "VND-SINO002", "CN", "ops@sinofreight.cn");
        v2.setContactPhone("+86-10-5550200");
        v2.setPerformanceScore(BigDecimal.valueOf(78.3));
        v2.setOnTimeDeliveryRate(BigDecimal.valueOf(80.0));
        em.persist(v2);

        Vendor v3 = new Vendor("EuroTrans GmbH", "VND-EURO003", "DE", "info@eurotrans.de");
        v3.setContactPhone("+49-30-5550300");
        v3.setPerformanceScore(BigDecimal.valueOf(96.0));
        v3.setOnTimeDeliveryRate(BigDecimal.valueOf(97.5));
        em.persist(v3);

        LOGGER.log(Level.INFO, "[INIT] Created 3 sample vendors.");
    }

    private void createSampleInventory() {
        long count = em.createQuery("SELECT COUNT(i) FROM InventoryItem i", Long.class).getSingleResult();
        if (count > 0) return;

        InventoryItem item1 = new InventoryItem("ELC-001", "Industrial Circuit Boards", 500, 100, 200);
        item1.setUnitCost(BigDecimal.valueOf(45.00));
        item1.setCurrencyCode("USD");
        item1.setWarehouseLocation("Warehouse-A");
        item1.setHsCode("8534.00");
        em.persist(item1);

        InventoryItem item2 = new InventoryItem("MEC-002", "Hydraulic Pump Components", 80, 50, 100);
        item2.setUnitCost(BigDecimal.valueOf(320.00));
        item2.setCurrencyCode("USD");
        item2.setWarehouseLocation("Warehouse-B");
        item2.setHsCode("8413.91");
        em.persist(item2);

        InventoryItem item3 = new InventoryItem("PKG-003", "Industrial Packaging Materials", 30, 50, 200);
        item3.setUnitCost(BigDecimal.valueOf(2.50));
        item3.setCurrencyCode("USD");
        item3.setWarehouseLocation("Warehouse-A");
        item3.setHsCode("4819.10");
        em.persist(item3);

        InventoryItem item4 = new InventoryItem("CHM-004", "Chemical Reagents - Grade A", 150, 75, 150);
        item4.setUnitCost(BigDecimal.valueOf(88.00));
        item4.setCurrencyCode("USD");
        item4.setWarehouseLocation("Warehouse-C");
        item4.setHsCode("2804.10");
        em.persist(item4);

        LOGGER.log(Level.INFO, "[INIT] Created 4 sample inventory items.");
    }

    private void createSampleShipments() {
        long count = em.createQuery("SELECT COUNT(s) FROM Shipment s", Long.class).getSingleResult();
        if (count > 0) return;

        Shipment s1 = new Shipment();
        s1.setTrackingNumber("GT-DEMO00001");
        s1.setOriginCountry("China");
        s1.setDestinationCountry("United States");
        s1.setCarrierName("GlobalShip Express");
        s1.setWeightKg(BigDecimal.valueOf(250.0));
        s1.setDeclaredValue(BigDecimal.valueOf(15000.00));
        s1.setCurrencyCode("USD");
        s1.setEstimatedDelivery(LocalDateTime.now().plusDays(7));
        s1.setStatus(ShipmentStatus.IN_TRANSIT);
        s1.setDescription("Industrial circuit boards - batch shipment");
        em.persist(s1);

        Shipment s2 = new Shipment();
        s2.setTrackingNumber("GT-DEMO00002");
        s2.setOriginCountry("Germany");
        s2.setDestinationCountry("Sri Lanka");
        s2.setCarrierName("EuroFreight");
        s2.setWeightKg(BigDecimal.valueOf(1200.0));
        s2.setDeclaredValue(BigDecimal.valueOf(85000.00));
        s2.setCurrencyCode("EUR");
        s2.setEstimatedDelivery(LocalDateTime.now().plusDays(14));
        s2.setStatus(ShipmentStatus.CUSTOMS_HOLD);
        s2.setDescription("Hydraulic machinery components");
        em.persist(s2);

        Shipment s3 = new Shipment();
        s3.setTrackingNumber("GT-DEMO00003");
        s3.setOriginCountry("United States");
        s3.setDestinationCountry("Japan");
        s3.setCarrierName("Pacific Cargo");
        s3.setWeightKg(BigDecimal.valueOf(75.5));
        s3.setDeclaredValue(BigDecimal.valueOf(4200.00));
        s3.setCurrencyCode("USD");
        s3.setEstimatedDelivery(LocalDateTime.now().minusDays(2));
        s3.setStatus(ShipmentStatus.IN_TRANSIT);
        s3.setDescription("Electronic components — overdue for delay check");
        em.persist(s3);

        LOGGER.log(Level.INFO, "[INIT] Created 3 sample shipments.");
    }

    private String hashPassword(String rawPassword) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawPassword.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
