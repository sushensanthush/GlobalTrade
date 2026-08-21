package com.global.session.vendor;

import com.global.entity.Vendor;
import com.global.exception.VendorValidationException;
import com.global.interceptor.Audited;
import com.global.interceptor.PerformanceMonitored;
import com.global.interceptor.ValidatedInput;
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
public class VendorManagementBean {

    private static final Logger LOGGER = Logger.getLogger(VendorManagementBean.class.getName());

    @PersistenceContext(unitName = "globalTradePU")
    private EntityManager em;


    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    @ValidatedInput
    public Vendor createVendor(String name, String countryCode, String contactEmail,
                                String contactPhone) throws VendorValidationException {
        validateVendorEmail(contactEmail);

        String vendorCode = "VND-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Vendor vendor = new Vendor(name, vendorCode, countryCode, contactEmail);
        vendor.setContactPhone(contactPhone);
        em.persist(vendor);

        LOGGER.log(Level.INFO, "[VENDOR] Created vendor {0} ({1}) in country {2}",
                new Object[]{name, vendorCode, countryCode});
        return vendor;
    }


    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public Vendor updatePerformanceScore(Long vendorId, BigDecimal performanceScore,
                                          BigDecimal onTimeDeliveryRate) throws VendorValidationException {
        if (performanceScore == null || performanceScore.compareTo(BigDecimal.ZERO) < 0
                || performanceScore.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new VendorValidationException("performanceScore",
                    "Performance score must be between 0 and 100");
        }

        Vendor vendor = em.find(Vendor.class, vendorId);
        if (vendor == null) {
            throw new VendorValidationException("vendorId", "Vendor not found with ID: " + vendorId);
        }

        vendor.setPerformanceScore(performanceScore);
        if (onTimeDeliveryRate != null) {
            vendor.setOnTimeDeliveryRate(onTimeDeliveryRate);
        }
        vendor.setLastEvaluated(LocalDateTime.now());
        em.merge(vendor);

        LOGGER.log(Level.INFO, "[VENDOR] Updated performance for {0}: score={1}, OTD={2}",
                new Object[]{vendor.getName(), performanceScore, onTimeDeliveryRate});
        return vendor;
    }


    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<Vendor> findAll() {
        return em.createNamedQuery("Vendor.findAll", Vendor.class).getResultList();
    }


    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<Vendor> findByCountry(String countryCode) {
        return em.createNamedQuery("Vendor.findByCountry", Vendor.class)
                .setParameter("countryCode", countryCode)
                .getResultList();
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public Vendor findById(Long id) {
        return em.find(Vendor.class, id);
    }


    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void deactivateVendor(Long vendorId) throws VendorValidationException {
        Vendor vendor = em.find(Vendor.class, vendorId);
        if (vendor == null) {
            throw new VendorValidationException("vendorId", "Vendor not found: " + vendorId);
        }
        vendor.setActive(false);
        em.merge(vendor);
        LOGGER.log(Level.WARNING, "[VENDOR] Vendor {0} ({1}) deactivated",
                new Object[]{vendor.getName(), vendor.getVendorCode()});
    }

    private void validateVendorEmail(String email) throws VendorValidationException {
        if (email == null || !email.contains("@") || !email.contains(".")) {
            throw new VendorValidationException("contactEmail",
                    "A valid email address is required for vendor registration");
        }
    }
}
