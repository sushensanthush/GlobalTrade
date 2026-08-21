package com.global.servlet;

import com.global.entity.Shipment;
import com.global.entity.ShipmentStatus;
import com.global.entity.User;
import com.global.entity.UserRole;
import com.global.exception.CustomsComplianceException;
import com.global.exception.ShipmentNotFoundException;
import com.global.exception.SupplyChainException;
import com.global.exception.UnauthorizedAccessException;
import com.global.session.auth.AuthenticationBean;
import com.global.session.shipment.ShipmentManagementBean;
import jakarta.ejb.EJB;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@WebServlet(name = "ShipmentServlet", urlPatterns = {"/ShipmentServlet"})
public class ShipmentServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(ShipmentServlet.class.getName());

    @EJB
    private ShipmentManagementBean shipmentBean;

    @EJB
    private AuthenticationBean authBean;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String action = request.getParameter("action");
        HttpSession session = request.getSession(false);
        User currentUser = (User) session.getAttribute("currentUser");

        try {
            if ("list".equals(action) || action == null) {
                List<Shipment> shipments = shipmentBean.findAll();
                request.setAttribute("shipments", shipments);
                request.getRequestDispatcher("/views/shipments.jsp").forward(request, response);

            } else if ("view".equals(action)) {
                String id = request.getParameter("id");
                if (id != null) {
                    Shipment shipment = shipmentBean.findById(Long.parseLong(id));
                    request.setAttribute("shipment", shipment);
                    request.getRequestDispatcher("/views/shipment-detail.jsp").forward(request, response);
                }

            } else if ("delete".equals(action)) {
                authBean.assertRole(currentUser, UserRole.LOGISTICS_COORDINATOR, UserRole.SYSTEM_ADMIN);
                String id = request.getParameter("id");
                if (id != null) {
                    shipmentBean.cancelShipment(Long.parseLong(id), "Cancelled by " + currentUser.getUsername());
                }
                response.sendRedirect(request.getContextPath() + "/ShipmentServlet?action=list&msg=cancelled");
            }

        } catch (ShipmentNotFoundException e) {
            request.setAttribute("error", "Shipment not found: " + e.getMessage());
            request.getRequestDispatcher("/error.jsp").forward(request, response);
        } catch (UnauthorizedAccessException e) {
            response.sendRedirect(request.getContextPath() + "/dashboard.jsp?error=access_denied");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "[SHIPMENT] Error in doGet", e);
            request.setAttribute("error", e.getMessage());
            request.getRequestDispatcher("/error.jsp").forward(request, response);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String action = request.getParameter("action");
        HttpSession session = request.getSession(false);
        User currentUser = (User) session.getAttribute("currentUser");

        try {
            if ("create".equals(action)) {
                authBean.assertRole(currentUser,
                        UserRole.LOGISTICS_COORDINATOR, UserRole.SYSTEM_ADMIN);

                String originCountry = request.getParameter("originCountry");
                String destinationCountry = request.getParameter("destinationCountry");
                String carrierName = request.getParameter("carrierName");
                String weightStr = request.getParameter("weightKg");
                String valueStr = request.getParameter("declaredValue");
                String currency = request.getParameter("currencyCode");
                String daysStr = request.getParameter("estimatedDays");
                String vendorIdStr = request.getParameter("vendorId");
                String description = request.getParameter("description");

                BigDecimal weight = weightStr != null && !weightStr.isBlank()
                        ? new BigDecimal(weightStr) : null;
                BigDecimal value = valueStr != null && !valueStr.isBlank()
                        ? new BigDecimal(valueStr) : BigDecimal.ZERO;
                int days = daysStr != null && !daysStr.isBlank() ? Integer.parseInt(daysStr) : 14;
                Long vendorId = vendorIdStr != null && !vendorIdStr.isBlank()
                        ? Long.parseLong(vendorIdStr) : null;

                Shipment shipment = shipmentBean.createShipment(
                        originCountry, destinationCountry, carrierName,
                        weight, value, currency,
                        LocalDateTime.now().plusDays(days), vendorId, description);

                response.sendRedirect(request.getContextPath()
                        + "/ShipmentServlet?action=view&id=" + shipment.getId() + "&msg=created");

            } else if ("updateStatus".equals(action)) {
                authBean.assertRole(currentUser,
                        UserRole.LOGISTICS_COORDINATOR, UserRole.CUSTOMS_AGENT, UserRole.SYSTEM_ADMIN);

                Long id = Long.parseLong(request.getParameter("id"));
                ShipmentStatus newStatus = ShipmentStatus.valueOf(request.getParameter("status"));
                shipmentBean.updateStatus(id, newStatus);
                response.sendRedirect(request.getContextPath()
                        + "/ShipmentServlet?action=view&id=" + id + "&msg=updated");

            } else if ("clearCustoms".equals(action)) {
                authBean.assertRole(currentUser, UserRole.CUSTOMS_AGENT, UserRole.SYSTEM_ADMIN);

                Long id = Long.parseLong(request.getParameter("id"));
                String declarationNumber = request.getParameter("declarationNumber");
                shipmentBean.processCustomsClearance(id, declarationNumber);
                response.sendRedirect(request.getContextPath()
                        + "/ShipmentServlet?action=view&id=" + id + "&msg=customs_cleared");
            }

        } catch (UnauthorizedAccessException e) {
            response.sendRedirect(request.getContextPath() + "/dashboard.jsp?error=access_denied");
        } catch (SupplyChainException e) {
            request.setAttribute("error", e.getMessage());
            request.getRequestDispatcher("/error.jsp").forward(request, response);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "[SHIPMENT] Error in doPost", e);
            request.setAttribute("error", "An unexpected error occurred: " + e.getMessage());
            request.getRequestDispatcher("/error.jsp").forward(request, response);
        }
    }
}
