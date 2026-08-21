package com.global.rest;

import com.global.entity.Shipment;
import com.global.entity.ShipmentStatus;
import com.global.exception.CustomsComplianceException;
import com.global.exception.ShipmentNotFoundException;
import com.global.exception.SupplyChainException;
import com.global.session.shipment.ShipmentManagementBean;
import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;


@Path("/shipments")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ShipmentResource {

    private static final Logger LOGGER = Logger.getLogger(ShipmentResource.class.getName());

    @EJB
    private ShipmentManagementBean shipmentBean;

    @GET
    public Response getAllShipments() {
        try {
            List<Shipment> shipments = shipmentBean.findAll();
            return Response.ok(shipments.stream().map(this::toDto).toList()).build();
        } catch (Exception e) {
            return serverError(e);
        }
    }

    @GET
    @Path("/{id}")
    public Response getShipmentById(@PathParam("id") Long id) {
        try {
            Shipment shipment = shipmentBean.findById(id);
            return Response.ok(toDto(shipment)).build();
        } catch (ShipmentNotFoundException e) {
            return notFound(e.getMessage());
        } catch (Exception e) {
            return serverError(e);
        }
    }

    @GET
    @Path("/track/{trackingNumber}")
    public Response trackShipment(@PathParam("trackingNumber") String trackingNumber) {
        try {
            Shipment shipment = shipmentBean.findByTrackingNumber(trackingNumber);
            return Response.ok(toDto(shipment)).build();
        } catch (ShipmentNotFoundException e) {
            return notFound(e.getMessage());
        } catch (Exception e) {
            return serverError(e);
        }
    }

    @GET
    @Path("/status/{status}")
    public Response getShipmentsByStatus(@PathParam("status") String statusStr) {
        try {
            ShipmentStatus status = ShipmentStatus.valueOf(statusStr.toUpperCase());
            List<Shipment> shipments = shipmentBean.findByStatus(status);
            return Response.ok(shipments.stream().map(this::toDto).toList()).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Invalid status value: " + statusStr,
                            "validValues", List.of(ShipmentStatus.values())))
                    .build();
        } catch (Exception e) {
            return serverError(e);
        }
    }

    @POST
    public Response createShipment(CreateShipmentRequest req) {
        try {
            if (req == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "Request body is required")).build();
            }
            Shipment shipment = shipmentBean.createShipment(
                    req.originCountry(), req.destinationCountry(), req.carrierName(),
                    req.weightKg() != null ? BigDecimal.valueOf(req.weightKg()) : null,
                    req.declaredValue() != null ? BigDecimal.valueOf(req.declaredValue()) : BigDecimal.ZERO,
                    req.currencyCode() != null ? req.currencyCode() : "USD",
                    req.estimatedDays() != null
                            ? LocalDateTime.now().plusDays(req.estimatedDays()) : LocalDateTime.now().plusDays(14),
                    req.vendorId(), req.description());
            return Response.status(Response.Status.CREATED).entity(toDto(shipment)).build();
        } catch (SupplyChainException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", e.getMessage(), "code", e.getErrorCode())).build();
        } catch (Exception e) {
            return serverError(e);
        }
    }

    @PUT
    @Path("/{id}/status")
    public Response updateStatus(@PathParam("id") Long id, UpdateStatusRequest req) {
        try {
            ShipmentStatus newStatus = ShipmentStatus.valueOf(req.status().toUpperCase());
            Shipment shipment = shipmentBean.updateStatus(id, newStatus);
            return Response.ok(toDto(shipment)).build();
        } catch (ShipmentNotFoundException e) {
            return notFound(e.getMessage());
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Invalid status: " + req.status())).build();
        } catch (Exception e) {
            return serverError(e);
        }
    }

    @PUT
    @Path("/{id}/customs")
    public Response processCustomsClearance(@PathParam("id") Long id, CustomsClearanceRequest req) {
        try {
            Shipment shipment = shipmentBean.processCustomsClearance(id, req.declarationNumber());
            return Response.ok(toDto(shipment)).build();
        } catch (ShipmentNotFoundException e) {
            return notFound(e.getMessage());
        } catch (CustomsComplianceException e) {
            return Response.status(422)
                    .entity(Map.of("error", e.getMessage(), "code", e.getErrorCode())).build();
        } catch (Exception e) {
            return serverError(e);
        }
    }

    @DELETE
    @Path("/{id}")
    public Response cancelShipment(@PathParam("id") Long id,
                                    @QueryParam("reason") @DefaultValue("Cancelled via API") String reason) {
        try {
            shipmentBean.cancelShipment(id, reason);
            return Response.ok(Map.of("message", "Shipment cancelled successfully", "id", id)).build();
        } catch (ShipmentNotFoundException e) {
            return notFound(e.getMessage());
        } catch (Exception e) {
            return serverError(e);
        }
    }


    private ShipmentDto toDto(Shipment s) {
        return new ShipmentDto(
                s.getId(), s.getTrackingNumber(), s.getStatus().name(),
                s.getOriginCountry(), s.getDestinationCountry(), s.getCarrierName(),
                s.getWeightKg(), s.getDeclaredValue(), s.getCurrencyCode(),
                s.getEstimatedDelivery() != null ? s.getEstimatedDelivery().toString() : null,
                s.getActualDelivery() != null ? s.getActualDelivery().toString() : null,
                s.isCustomsCleared(), s.getCustomsDeclarationNumber(),
                s.getDescription(),
                s.getCreatedAt() != null ? s.getCreatedAt().toString() : null
        );
    }

    private Response notFound(String message) {
        return Response.status(Response.Status.NOT_FOUND)
                .entity(Map.of("error", message)).build();
    }

    private Response serverError(Exception e) {
        LOGGER.log(Level.SEVERE, "[REST] Internal error", e);
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Map.of("error", "Internal server error: " + e.getMessage())).build();
    }


    public record CreateShipmentRequest(
            String originCountry, String destinationCountry, String carrierName,
            Double weightKg, Double declaredValue, String currencyCode,
            Integer estimatedDays, Long vendorId, String description) {}

    public record UpdateStatusRequest(String status) {}

    public record CustomsClearanceRequest(String declarationNumber) {}

    public record ShipmentDto(
            Long id, String trackingNumber, String status,
            String originCountry, String destinationCountry, String carrierName,
            BigDecimal weightKg, BigDecimal declaredValue, String currencyCode,
            String estimatedDelivery, String actualDelivery,
            boolean customsCleared, String customsDeclarationNumber,
            String description, String createdAt) {}
}
