package com.global.servlet;

import com.global.entity.InventoryItem;
import com.global.entity.User;
import com.global.entity.UserRole;
import com.global.exception.InsufficientInventoryException;
import com.global.exception.SupplyChainException;
import com.global.exception.UnauthorizedAccessException;
import com.global.session.auth.AuthenticationBean;
import com.global.session.inventory.InventoryManagementBean;
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

@WebServlet(name = "InventoryServlet", urlPatterns = {"/InventoryServlet"})
public class InventoryServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(InventoryServlet.class.getName());

    @EJB
    private InventoryManagementBean inventoryBean;

    @EJB
    private AuthenticationBean authBean;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String action = request.getParameter("action");

        try {
            List<InventoryItem> items = inventoryBean.findAll();
            request.setAttribute("items", items);

            List<InventoryItem> lowStock = inventoryBean.findLowStockItems();
            request.setAttribute("lowStockItems", lowStock);

            request.getRequestDispatcher("/views/inventory.jsp").forward(request, response);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "[INVENTORY] Error in doGet", e);
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
                        UserRole.WAREHOUSE_MANAGER, UserRole.SYSTEM_ADMIN);

                String itemCode = request.getParameter("itemCode");
                String itemName = request.getParameter("itemName");
                int qty = Integer.parseInt(request.getParameter("quantityOnHand"));
                int reorderPt = Integer.parseInt(request.getParameter("reorderPoint"));
                int reorderQty = Integer.parseInt(request.getParameter("reorderQuantity"));
                String costStr = request.getParameter("unitCost");
                BigDecimal cost = costStr != null && !costStr.isBlank()
                        ? new BigDecimal(costStr) : BigDecimal.ZERO;
                String currency = request.getParameter("currencyCode");
                String warehouse = request.getParameter("warehouseLocation");
                String hsCode = request.getParameter("hsCode");

                inventoryBean.createItem(itemCode, itemName, qty, reorderPt, reorderQty,
                        cost, currency, warehouse, hsCode);
                response.sendRedirect(request.getContextPath() + "/InventoryServlet?msg=created");

            } else if ("deduct".equals(action)) {
                authBean.assertRole(currentUser,
                        UserRole.WAREHOUSE_MANAGER, UserRole.LOGISTICS_COORDINATOR, UserRole.SYSTEM_ADMIN);

                String itemCode = request.getParameter("itemCode");
                int quantity = Integer.parseInt(request.getParameter("quantity"));
                inventoryBean.deductStock(itemCode, quantity);
                response.sendRedirect(request.getContextPath() + "/InventoryServlet?msg=deducted");

            } else if ("replenish".equals(action)) {
                authBean.assertRole(currentUser,
                        UserRole.WAREHOUSE_MANAGER, UserRole.SYSTEM_ADMIN);

                String itemCode = request.getParameter("itemCode");
                int quantity = Integer.parseInt(request.getParameter("quantity"));
                inventoryBean.replenishStock(itemCode, quantity);
                response.sendRedirect(request.getContextPath() + "/InventoryServlet?msg=replenished");
            }

        } catch (UnauthorizedAccessException e) {
            response.sendRedirect(request.getContextPath() + "/dashboard.jsp?error=access_denied");
        } catch (InsufficientInventoryException e) {
            request.setAttribute("error", "Insufficient stock: " + e.getMessage());
            request.getRequestDispatcher("/error.jsp").forward(request, response);
        } catch (SupplyChainException e) {
            request.setAttribute("error", e.getMessage());
            request.getRequestDispatcher("/error.jsp").forward(request, response);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "[INVENTORY] Error in doPost", e);
            request.setAttribute("error", "Unexpected error: " + e.getMessage());
            request.getRequestDispatcher("/error.jsp").forward(request, response);
        }
    }
}
