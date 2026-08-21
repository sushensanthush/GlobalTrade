package com.global.rest;

import com.global.entity.InventoryItem;
import com.global.exception.InsufficientInventoryException;
import com.global.exception.SupplyChainException;
import com.global.session.inventory.InventoryManagementBean;
import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@Path("/inventory")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class InventoryResource {

    private static final Logger LOGGER = Logger.getLogger(InventoryResource.class.getName());

    @EJB
    private InventoryManagementBean inventoryBean;

    @GET
    public Response getAllItems() {
        try {
            List<InventoryItem> items = inventoryBean.findAll();
            return Response.ok(items.stream().map(this::toDto).toList()).build();
        } catch (Exception e) {
            return serverError(e);
        }
    }

    @GET
    @Path("/lowstock")
    public Response getLowStockItems() {
        try {
            List<InventoryItem> items = inventoryBean.findLowStockItems();
            return Response.ok(Map.of(
                    "count", items.size(),
                    "items", items.stream().map(this::toDto).toList())).build();
        } catch (Exception e) {
            return serverError(e);
        }
    }

    @GET
    @Path("/{itemCode}")
    public Response getItemByCode(@PathParam("itemCode") String itemCode) {
        try {
            InventoryItem item = inventoryBean.findByItemCode(itemCode);
            if (item == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("error", "Item not found: " + itemCode)).build();
            }
            return Response.ok(toDto(item)).build();
        } catch (Exception e) {
            return serverError(e);
        }
    }

    @POST
    public Response createItem(CreateItemRequest req) {
        try {
            if (req == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "Request body is required")).build();
            }
            InventoryItem item = inventoryBean.createItem(
                    req.itemCode(), req.itemName(), req.quantityOnHand(),
                    req.reorderPoint(), req.reorderQuantity(),
                    req.unitCost() != null ? BigDecimal.valueOf(req.unitCost()) : BigDecimal.ZERO,
                    req.currencyCode() != null ? req.currencyCode() : "USD",
                    req.warehouseLocation(), req.hsCode());
            return Response.status(Response.Status.CREATED).entity(toDto(item)).build();
        } catch (Exception e) {
            return serverError(e);
        }
    }

    @PUT
    @Path("/{itemCode}/deduct")
    public Response deductStock(@PathParam("itemCode") String itemCode, StockOperationRequest req) {
        try {
            InventoryItem item = inventoryBean.deductStock(itemCode, req.quantity());
            return Response.ok(Map.of(
                    "message", "Stock deducted successfully",
                    "itemCode", itemCode,
                    "quantityDeducted", req.quantity(),
                    "newQuantityOnHand", item.getQuantityOnHand())).build();
        } catch (InsufficientInventoryException e) {
            return Response.status(422)
                    .entity(Map.of("error", e.getMessage(),
                            "code", e.getErrorCode(),
                            "requested", e.getRequested(),
                            "available", e.getAvailable())).build();
        } catch (SupplyChainException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", e.getMessage())).build();
        } catch (Exception e) {
            return serverError(e);
        }
    }

    @PUT
    @Path("/{itemCode}/replenish")
    public Response replenishStock(@PathParam("itemCode") String itemCode, StockOperationRequest req) {
        try {
            InventoryItem item = inventoryBean.replenishStock(itemCode, req.quantity());
            return Response.ok(Map.of(
                    "message", "Stock replenished successfully",
                    "itemCode", itemCode,
                    "quantityAdded", req.quantity(),
                    "newQuantityOnHand", item.getQuantityOnHand())).build();
        } catch (SupplyChainException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", e.getMessage())).build();
        } catch (Exception e) {
            return serverError(e);
        }
    }

    private InventoryDto toDto(InventoryItem i) {
        return new InventoryDto(
                i.getId(), i.getItemCode(), i.getItemName(), i.getDescription(),
                i.getQuantityOnHand(), i.getReorderPoint(), i.getReorderQuantity(),
                i.getUnitCost(), i.getCurrencyCode(), i.getWarehouseLocation(),
                i.getHsCode(), i.isActive(),
                i.getLastReplenishment() != null ? i.getLastReplenishment().toString() : null,
                i.getQuantityOnHand() <= i.getReorderPoint());
    }

    private Response serverError(Exception e) {
        LOGGER.log(Level.SEVERE, "[REST] Inventory API error", e);
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Map.of("error", "Internal server error: " + e.getMessage())).build();
    }

    public record CreateItemRequest(
            String itemCode, String itemName, int quantityOnHand,
            int reorderPoint, int reorderQuantity, Double unitCost,
            String currencyCode, String warehouseLocation, String hsCode) {}

    public record StockOperationRequest(int quantity) {}

    public record InventoryDto(
            Long id, String itemCode, String itemName, String description,
            int quantityOnHand, int reorderPoint, int reorderQuantity,
            BigDecimal unitCost, String currencyCode, String warehouseLocation,
            String hsCode, boolean active, String lastReplenishment, boolean lowStock) {}
}
