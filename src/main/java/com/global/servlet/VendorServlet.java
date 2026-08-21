package com.global.servlet;

import com.global.entity.User;
import com.global.entity.UserRole;
import com.global.entity.Vendor;
import com.global.exception.UnauthorizedAccessException;
import com.global.exception.VendorValidationException;
import com.global.session.auth.AuthenticationBean;
import com.global.session.vendor.VendorManagementBean;
import jakarta.ejb.EJB;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@WebServlet(name = "VendorServlet", urlPatterns = {"/VendorServlet"})
public class VendorServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(VendorServlet.class.getName());

    @EJB
    private VendorManagementBean vendorBean;

    @EJB
    private AuthenticationBean authBean;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String action = request.getParameter("action");

        try {
            if ("list".equals(action) || action == null) {
                List<Vendor> vendors = vendorBean.findAll();
                request.setAttribute("vendors", vendors);
                request.getRequestDispatcher("/views/vendors.jsp").forward(request, response);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "[VENDOR] Error in doGet", e);
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

                String name = request.getParameter("name");
                String countryCode = request.getParameter("countryCode");
                String email = request.getParameter("contactEmail");
                String phone = request.getParameter("contactPhone");

                vendorBean.createVendor(name, countryCode, email, phone);
                response.sendRedirect(request.getContextPath() + "/VendorServlet?action=list&msg=created");

            } else if ("updateScore".equals(action)) {
                authBean.assertRole(currentUser,
                        UserRole.LOGISTICS_COORDINATOR, UserRole.SYSTEM_ADMIN);

                Long vendorId = Long.parseLong(request.getParameter("vendorId"));
                BigDecimal score = new BigDecimal(request.getParameter("performanceScore"));
                BigDecimal otd = new BigDecimal(request.getParameter("onTimeDeliveryRate"));
                vendorBean.updatePerformanceScore(vendorId, score, otd);
                response.sendRedirect(request.getContextPath() + "/VendorServlet?action=list&msg=updated");
            }

        } catch (UnauthorizedAccessException e) {
            response.sendRedirect(request.getContextPath() + "/dashboard.jsp?error=access_denied");
        } catch (VendorValidationException e) {
            request.setAttribute("error", e.getMessage());
            request.getRequestDispatcher("/error.jsp").forward(request, response);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "[VENDOR] Error in doPost", e);
            request.setAttribute("error", "Unexpected error: " + e.getMessage());
            request.getRequestDispatcher("/error.jsp").forward(request, response);
        }
    }
}
