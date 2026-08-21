package com.global.servlet;

import com.global.entity.AuditLog;
import com.global.entity.InventoryItem;
import com.global.entity.Shipment;
import com.global.entity.ShipmentStatus;
import com.global.entity.TimerLog;
import com.global.entity.Vendor;

import com.global.session.audit.AuditLogBean;
import com.global.session.inventory.InventoryManagementBean;
import com.global.session.shipment.ShipmentManagementBean;
import com.global.session.vendor.VendorManagementBean;

import jakarta.ejb.EJB;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@WebServlet(
        name = "DashboardServlet",
        urlPatterns = {"/DashboardServlet"}
)
public class DashboardServlet extends HttpServlet {

    private static final Logger LOGGER =
            Logger.getLogger(
                    DashboardServlet.class.getName()
            );

    @EJB
    private ShipmentManagementBean shipmentBean;

    @EJB
    private InventoryManagementBean inventoryBean;

    @EJB
    private VendorManagementBean vendorBean;

    @EJB
    private AuditLogBean auditLogBean;

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        try {

            List<Shipment> allShipments =
                    shipmentBean.findAll();

            if (allShipments == null) {
                allShipments = List.of();
            }

            Map<ShipmentStatus, Long> shipmentsByStatus =
                    allShipments.stream()
                            .filter(s -> s != null)
                            .filter(s -> s.getStatus() != null)
                            .collect(
                                    Collectors.groupingBy(
                                            Shipment::getStatus,
                                            Collectors.counting()
                                    )
                            );

            long delayedCount =
                    allShipments.stream()
                            .filter(s -> s != null)
                            .filter(s -> s.getStatus()
                                    == ShipmentStatus.DELAYED)
                            .count();

            long inTransitCount =
                    allShipments.stream()
                            .filter(s -> s != null)
                            .filter(s -> s.getStatus()
                                    == ShipmentStatus.IN_TRANSIT)
                            .count();

            long customsHoldCount =
                    allShipments.stream()
                            .filter(s -> s != null)
                            .filter(s -> s.getStatus()
                                    == ShipmentStatus.CUSTOMS_HOLD)
                            .count();


            List<Shipment> recentShipments =
                    allShipments.stream()
                            .filter(s -> s != null)
                            .limit(5)
                            .collect(Collectors.toList());




            List<InventoryItem> allItems =
                    inventoryBean.findAll();

            if (allItems == null) {
                allItems = List.of();
            }

            List<InventoryItem> lowStockItems =
                    inventoryBean.findLowStockItems();

            if (lowStockItems == null) {
                lowStockItems = List.of();
            }



            List<Vendor> allVendors =
                    vendorBean.findAll();

            if (allVendors == null) {
                allVendors = List.of();
            }

            long activeVendors =
                    allVendors.stream()
                            .filter(v -> v != null)
                            .filter(Vendor::isActive)
                            .count();

            long underperformingVendors =
                    allVendors.stream()
                            .filter(v -> v != null)
                            .filter(Vendor::isActive)
                            .filter(v ->
                                    v.getPerformanceScore() != null
                                            && v.getPerformanceScore()
                                            .compareTo(
                                                    BigDecimal.valueOf(75)
                                            ) < 0
                            )
                            .count();




            List<AuditLog> recentAuditLogs =
                    auditLogBean.findRecentAuditLogs(6);

            if (recentAuditLogs == null) {
                recentAuditLogs = List.of();
            }




            List<TimerLog> recentTimerLogs =
                    auditLogBean.findRecentTimerLogs(6);

            if (recentTimerLogs == null) {
                recentTimerLogs = List.of();
            }




            request.setAttribute(
                    "totalShipments",
                    allShipments.size()
            );

            request.setAttribute(
                    "delayedCount",
                    delayedCount
            );

            request.setAttribute(
                    "inTransitCount",
                    inTransitCount
            );

            request.setAttribute(
                    "customsHoldCount",
                    customsHoldCount
            );

            request.setAttribute(
                    "shipmentsByStatus",
                    shipmentsByStatus
            );

            request.setAttribute(
                    "recentShipments",
                    recentShipments
            );



            request.setAttribute(
                    "totalInventoryItems",
                    allItems.size()
            );

            request.setAttribute(
                    "lowStockCount",
                    lowStockItems.size()
            );

            request.setAttribute(
                    "lowStockItems",
                    lowStockItems
            );


            request.setAttribute(
                    "totalVendors",
                    allVendors.size()
            );

            request.setAttribute(
                    "activeVendors",
                    activeVendors
            );

            request.setAttribute(
                    "underperformingVendors",
                    underperformingVendors
            );



            request.setAttribute(
                    "recentAuditLogs",
                    recentAuditLogs.stream()
                            .limit(10)
                            .collect(Collectors.toList())
            );


            request.setAttribute(
                    "recentTimerLogs",
                    recentTimerLogs.stream()
                            .limit(5)
                            .collect(Collectors.toList())
            );




            request.getRequestDispatcher(
                    "/dashboard.jsp"
            ).forward(
                    request,
                    response
            );

        } catch (Exception e) {


            LOGGER.log(
                    Level.SEVERE,
                    "[DASHBOARD] Error loading dashboard data",
                    e
            );


            request.setAttribute(
                    "error",
                    "Dashboard data unavailable: "
                            + (e.getMessage() != null
                            ? e.getMessage()
                            : e.getClass().getSimpleName())
            );

            request.getRequestDispatcher(
                    "/dashboard.jsp"
            ).forward(
                    request,
                    response
            );
        }
    }
}