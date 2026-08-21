package com.global.session.inventory;

import com.global.entity.InventoryItem;
import com.global.exception.InsufficientInventoryException;
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
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
@Audited
@PerformanceMonitored
public class InventoryManagementBean {

    private static final Logger LOGGER =
            Logger.getLogger(InventoryManagementBean.class.getName());

    @PersistenceContext(unitName = "globalTradePU")
    private EntityManager em;

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public InventoryItem createItem(
            String itemCode,
            String itemName,
            int initialQuantity,
            int reorderPoint,
            int reorderQuantity,
            BigDecimal unitCost,
            String currencyCode,
            String warehouseLocation,
            String hsCode) {

        InventoryItem item = new InventoryItem(
                itemCode,
                itemName,
                initialQuantity,
                reorderPoint,
                reorderQuantity
        );

        item.setUnitCost(unitCost);
        item.setCurrencyCode(currencyCode != null ? currencyCode : "USD");
        item.setWarehouseLocation(warehouseLocation);
        item.setHsCode(hsCode);

        em.persist(item);

        LOGGER.log(
                Level.INFO,
                "[INVENTORY] Created item {0} with initial qty {1} in warehouse {2}",
                new Object[]{itemCode, initialQuantity, warehouseLocation}
        );

        return item;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public InventoryItem deductStock(String itemCode, int quantity)
            throws InsufficientInventoryException, SupplyChainException {

        InventoryItem item = findByItemCodeForUpdate(itemCode);

        if (item == null) {
            throw new SupplyChainException(
                    "ITEM_NOT_FOUND",
                    "Inventory item not found: " + itemCode
            );
        }

        if (item.getQuantityOnHand() < quantity) {
            throw new InsufficientInventoryException(
                    itemCode,
                    quantity,
                    item.getQuantityOnHand()
            );
        }

        item.setQuantityOnHand(
                item.getQuantityOnHand() - quantity
        );

        em.merge(item);

        LOGGER.log(
                Level.INFO,
                "[INVENTORY] Deducted {0} units from {1}. Remaining: {2}",
                new Object[]{
                        quantity,
                        itemCode,
                        item.getQuantityOnHand()
                }
        );

        return item;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public InventoryItem replenishStock(
            String itemCode,
            int quantity) throws SupplyChainException {

        InventoryItem item = findByItemCode(itemCode);

        if (item == null) {
            throw new SupplyChainException(
                    "ITEM_NOT_FOUND",
                    "Inventory item not found: " + itemCode
            );
        }

        item.setQuantityOnHand(
                item.getQuantityOnHand() + quantity
        );

        item.setLastReplenishment(LocalDateTime.now());

        em.merge(item);

        LOGGER.log(
                Level.INFO,
                "[INVENTORY] Replenished {0} units for {1}. New total: {2}",
                new Object[]{
                        quantity,
                        itemCode,
                        item.getQuantityOnHand()
                }
        );

        return item;
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<InventoryItem> findAll() {
        return em.createNamedQuery(
                "InventoryItem.findAll",
                InventoryItem.class
        ).getResultList();
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public List<InventoryItem> findLowStockItems() {
        return em.createNamedQuery(
                "InventoryItem.findLowStock",
                InventoryItem.class
        ).getResultList();
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public InventoryItem findByItemCode(String itemCode) {

        List<InventoryItem> results = em
                .createNamedQuery(
                        "InventoryItem.findByItemCode",
                        InventoryItem.class
                )
                .setParameter("itemCode", itemCode)
                .getResultList();

        return results.isEmpty() ? null : results.get(0);
    }

    private InventoryItem findByItemCodeForUpdate(String itemCode) {

        List<InventoryItem> results = em
                .createNamedQuery(
                        "InventoryItem.findByItemCode",
                        InventoryItem.class
                )
                .setParameter("itemCode", itemCode)
                .getResultList();

        if (results.isEmpty()) {
            return null;
        }

        InventoryItem item = results.get(0);

        em.lock(
                item,
                jakarta.persistence.LockModeType.PESSIMISTIC_WRITE
        );

        return item;
    }
}