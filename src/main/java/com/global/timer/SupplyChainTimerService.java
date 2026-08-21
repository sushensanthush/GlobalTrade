package com.global.timer;

import com.global.entity.*;
import com.global.interceptor.Audited;
import com.global.interceptor.PerformanceMonitored;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.ejb.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDateTime;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


@Singleton
@Startup
@Audited
@PerformanceMonitored
public class SupplyChainTimerService {

    private static final Logger LOGGER = Logger.getLogger(SupplyChainTimerService.class.getName());

    @PersistenceContext(unitName = "globalTradePU")
    private EntityManager em;

    @Resource
    private TimerService timerService;


    @Schedule(second = "0,30", minute = "*", hour = "*", persistent = true,
              info = "ShipmentStatusMonitor")
    public void monitorShipmentStatus(Timer timer) {
        long startTime = System.currentTimeMillis();
        LOGGER.log(Level.INFO, "[TIMER] ShipmentStatusMonitor fired at {0}", LocalDateTime.now());
        int processed = 0;

        try {
            List<Shipment> delayedShipments = em.createNamedQuery("Shipment.findDelayed", Shipment.class)
                    .setParameter("now", LocalDateTime.now())
                    .getResultList();

            for (Shipment shipment : delayedShipments) {
                if (shipment.getStatus() == ShipmentStatus.IN_TRANSIT) {
                    shipment.setStatus(ShipmentStatus.DELAYED);
                    em.merge(shipment);
                    LOGGER.log(Level.WARNING,
                            "[TIMER] Shipment {0} marked as DELAYED — estimated delivery was {1}",
                            new Object[]{shipment.getTrackingNumber(), shipment.getEstimatedDelivery()});
                    processed++;
                }
            }

            persistTimerLog("ShipmentStatusMonitor",
                    "Processed " + processed + " delayed shipments",
                    System.currentTimeMillis() - startTime, true, processed, null);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "[TIMER] ShipmentStatusMonitor failed", e);
            persistTimerLog("ShipmentStatusMonitor", "Timer execution failed",
                    System.currentTimeMillis() - startTime, false, 0, e.getMessage());
        }
    }


    @Schedule(second = "0", minute = "*", hour = "*", persistent = true,
              info = "InventoryReplenishmentMonitor")
    public void monitorInventoryLevels(Timer timer) {
        long startTime = System.currentTimeMillis();
        LOGGER.log(Level.INFO, "[TIMER] InventoryReplenishmentMonitor fired at {0}", LocalDateTime.now());
        int processed = 0;

        try {
            List<InventoryItem> lowStockItems = em.createNamedQuery("InventoryItem.findLowStock", InventoryItem.class)
                    .getResultList();

            for (InventoryItem item : lowStockItems) {
                LOGGER.log(Level.WARNING,
                        "[TIMER] LOW STOCK ALERT — Item: {0}, On-hand: {1}, Reorder point: {2}. " +
                        "Initiating replenishment for qty: {3}",
                        new Object[]{item.getItemCode(), item.getQuantityOnHand(),
                                item.getReorderPoint(), item.getReorderQuantity()});
                item.setLastReplenishment(LocalDateTime.now());
                em.merge(item);
                processed++;
            }

            persistTimerLog("InventoryReplenishmentMonitor",
                    "Checked inventory levels, " + processed + " low-stock alerts raised",
                    System.currentTimeMillis() - startTime, true, processed, null);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "[TIMER] InventoryReplenishmentMonitor failed", e);
            persistTimerLog("InventoryReplenishmentMonitor", "Timer execution failed",
                    System.currentTimeMillis() - startTime, false, 0, e.getMessage());
        }
    }


    @Schedule(second = "0", minute = "*/2", hour = "*", persistent = true,
              info = "VendorPerformanceEvaluator")
    public void evaluateVendorPerformance(Timer timer) {
        long startTime = System.currentTimeMillis();
        LOGGER.log(Level.INFO, "[TIMER] VendorPerformanceEvaluator fired at {0}", LocalDateTime.now());
        int processed = 0;

        try {
            List<Vendor> underperformingVendors = em.createNamedQuery(
                    "Vendor.findUnderperforming", Vendor.class)
                    .setParameter("threshold", java.math.BigDecimal.valueOf(75.0))
                    .getResultList();

            for (Vendor vendor : underperformingVendors) {
                LOGGER.log(Level.WARNING,
                        "[TIMER] VENDOR ALERT — Vendor: {0} ({1}), Performance score: {2}. " +
                        "Performance review required.",
                        new Object[]{vendor.getName(), vendor.getVendorCode(),
                                vendor.getPerformanceScore()});
                vendor.setLastEvaluated(LocalDateTime.now());
                em.merge(vendor);
                processed++;
            }

            persistTimerLog("VendorPerformanceEvaluator",
                    "Evaluated vendors, " + processed + " underperforming alerts raised",
                    System.currentTimeMillis() - startTime, true, processed, null);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "[TIMER] VendorPerformanceEvaluator failed", e);
            persistTimerLog("VendorPerformanceEvaluator", "Timer execution failed",
                    System.currentTimeMillis() - startTime, false, 0, e.getMessage());
        }
    }


    public void scheduleCustomsDeadlineAlert(Long shipmentId, long delayMinutes) {
        long delayMs = delayMinutes * 60 * 1000;
        TimerConfig config = new TimerConfig("CustomsDeadline_" + shipmentId, true);
        timerService.createSingleActionTimer(delayMs, config);
        LOGGER.log(Level.INFO,
                "[TIMER] Programmatic customs deadline timer created for shipment {0}, " +
                "fires in {1} minutes", new Object[]{shipmentId, delayMinutes});
    }


    @Timeout
    public void handleProgrammaticTimeout(Timer timer) {
        String info = (String) timer.getInfo();
        LOGGER.log(Level.WARNING,
                "[TIMER] Programmatic timer fired — info: {0} at {1}",
                new Object[]{info, LocalDateTime.now()});

        if (info != null && info.startsWith("CustomsDeadline_")) {
            String shipmentIdStr = info.replace("CustomsDeadline_", "");
            try {
                Long shipmentId = Long.parseLong(shipmentIdStr);
                Shipment shipment = em.find(Shipment.class, shipmentId);
                if (shipment != null && !shipment.isCustomsCleared()) {
                    LOGGER.log(Level.SEVERE,
                            "[TIMER] CUSTOMS DEADLINE ALERT — Shipment {0} ({1}) has not cleared customs!",
                            new Object[]{shipment.getTrackingNumber(), shipmentId});
                }
            } catch (NumberFormatException e) {
                LOGGER.log(Level.WARNING, "[TIMER] Could not parse shipment ID from timer info: " + info);
            }
        }

        persistTimerLog("ProgrammaticTimer", "Timer fired: " + info,
                0L, true, 0, null);
    }

    private void persistTimerLog(String timerType, String description,
                                  long executionTimeMs, boolean success,
                                  int recordsProcessed, String errorMessage) {
        try {
            TimerLog log = new TimerLog(timerType, description);
            log.setExecutionTimeMs(executionTimeMs);
            log.setSuccess(success);
            log.setRecordsProcessed(recordsProcessed);
            log.setErrorMessage(errorMessage);
            em.persist(log);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "[TIMER] Failed to persist timer log: " + e.getMessage());
        }
    }
}
