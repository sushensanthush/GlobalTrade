package com.global.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_items")
@NamedQueries({
        @NamedQuery(name = "InventoryItem.findAll",
                query = "SELECT i FROM InventoryItem i ORDER BY i.itemCode ASC"),
        @NamedQuery(name = "InventoryItem.findByItemCode",
                query = "SELECT i FROM InventoryItem i WHERE i.itemCode = :itemCode"),
        @NamedQuery(name = "InventoryItem.findLowStock",
                query = "SELECT i FROM InventoryItem i WHERE i.quantityOnHand <= i.reorderPoint AND i.active = true"),
        @NamedQuery(name = "InventoryItem.findByWarehouse",
                query = "SELECT i FROM InventoryItem i WHERE i.warehouseLocation = :warehouse")
})
public class InventoryItem implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "item_code", unique = true, nullable = false, length = 50)
    private String itemCode;

    @NotBlank
    @Column(name = "item_name", nullable = false, length = 200)
    private String itemName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Min(0)
    @Column(name = "quantity_on_hand", nullable = false)
    private int quantityOnHand;

    @Column(name = "reorder_point", nullable = false)
    private int reorderPoint;

    @Column(name = "reorder_quantity", nullable = false)
    private int reorderQuantity;

    @Column(name = "unit_cost", precision = 15, scale = 2)
    private BigDecimal unitCost;

    @Column(name = "currency_code", length = 3)
    private String currencyCode;

    @Column(name = "warehouse_location", length = 100)
    private String warehouseLocation;

    @Column(name = "hs_code", length = 20)
    private String hsCode;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "last_replenishment")
    private LocalDateTime lastReplenishment;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public InventoryItem() {}

    public InventoryItem(String itemCode, String itemName, int quantityOnHand, int reorderPoint, int reorderQuantity) {
        this.itemCode = itemCode;
        this.itemName = itemName;
        this.quantityOnHand = quantityOnHand;
        this.reorderPoint = reorderPoint;
        this.reorderQuantity = reorderQuantity;
    }


    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getItemCode() { return itemCode; }
    public void setItemCode(String itemCode) { this.itemCode = itemCode; }
    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public int getQuantityOnHand() { return quantityOnHand; }
    public void setQuantityOnHand(int quantityOnHand) { this.quantityOnHand = quantityOnHand; }
    public int getReorderPoint() { return reorderPoint; }
    public void setReorderPoint(int reorderPoint) { this.reorderPoint = reorderPoint; }
    public int getReorderQuantity() { return reorderQuantity; }
    public void setReorderQuantity(int reorderQuantity) { this.reorderQuantity = reorderQuantity; }
    public BigDecimal getUnitCost() { return unitCost; }
    public void setUnitCost(BigDecimal unitCost) { this.unitCost = unitCost; }
    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }
    public String getWarehouseLocation() { return warehouseLocation; }
    public void setWarehouseLocation(String warehouseLocation) { this.warehouseLocation = warehouseLocation; }
    public String getHsCode() { return hsCode; }
    public void setHsCode(String hsCode) { this.hsCode = hsCode; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public LocalDateTime getLastReplenishment() { return lastReplenishment; }
    public void setLastReplenishment(LocalDateTime lastReplenishment) { this.lastReplenishment = lastReplenishment; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
