package com.global.rest;

import com.global.entity.Vendor;
import com.global.exception.VendorValidationException;
import com.global.session.vendor.VendorManagementBean;
import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;


@Path("/vendors")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class VendorResource {

    private static final Logger LOGGER = Logger.getLogger(VendorResource.class.getName());

    @EJB
    private VendorManagementBean vendorBean;

    @GET
    public Response getAllVendors() {
        try {
            List<Vendor> vendors = vendorBean.findAll();
            return Response.ok(vendors.stream().map(this::toDto).toList()).build();
        } catch (Exception e) {
            return serverError(e);
        }
    }

    @GET
    @Path("/{id}")
    public Response getVendorById(@PathParam("id") Long id) {
        try {
            Vendor vendor = vendorBean.findById(id);
            if (vendor == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("error", "Vendor not found with ID: " + id)).build();
            }
            return Response.ok(toDto(vendor)).build();
        } catch (Exception e) {
            return serverError(e);
        }
    }

    @GET
    @Path("/country/{countryCode}")
    public Response getVendorsByCountry(@PathParam("countryCode") String countryCode) {
        try {
            List<Vendor> vendors = vendorBean.findByCountry(countryCode);
            return Response.ok(vendors.stream().map(this::toDto).toList()).build();
        } catch (Exception e) {
            return serverError(e);
        }
    }

    @POST
    public Response createVendor(CreateVendorRequest req) {
        try {
            if (req == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "Request body is required")).build();
            }
            Vendor vendor = vendorBean.createVendor(
                    req.name(), req.countryCode(), req.contactEmail(), req.contactPhone());
            return Response.status(Response.Status.CREATED).entity(toDto(vendor)).build();
        } catch (VendorValidationException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", e.getMessage(), "code", e.getErrorCode())).build();
        } catch (Exception e) {
            return serverError(e);
        }
    }

    @PUT
    @Path("/{id}/score")
    public Response updatePerformanceScore(@PathParam("id") Long id, UpdateScoreRequest req) {
        try {
            Vendor vendor = vendorBean.updatePerformanceScore(
                    id,
                    BigDecimal.valueOf(req.performanceScore()),
                    BigDecimal.valueOf(req.onTimeDeliveryRate()));
            return Response.ok(toDto(vendor)).build();
        } catch (VendorValidationException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", e.getMessage())).build();
        } catch (Exception e) {
            return serverError(e);
        }
    }

    private VendorDto toDto(Vendor v) {
        return new VendorDto(
                v.getId(), v.getName(), v.getVendorCode(), v.getCountryCode(),
                v.getContactEmail(), v.getContactPhone(),
                v.getPerformanceScore(), v.getOnTimeDeliveryRate(), v.isActive(),
                v.getCreatedAt() != null ? v.getCreatedAt().toString() : null,
                v.getLastEvaluated() != null ? v.getLastEvaluated().toString() : null);
    }

    private Response serverError(Exception e) {
        LOGGER.log(Level.SEVERE, "[REST] Vendor API error", e);
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Map.of("error", "Internal server error: " + e.getMessage())).build();
    }

    public record CreateVendorRequest(
            String name, String countryCode, String contactEmail, String contactPhone) {}

    public record UpdateScoreRequest(double performanceScore, double onTimeDeliveryRate) {}

    public record VendorDto(
            Long id, String name, String vendorCode, String countryCode,
            String contactEmail, String contactPhone,
            BigDecimal performanceScore, BigDecimal onTimeDeliveryRate,
            boolean active, String createdAt, String lastEvaluated) {}
}
