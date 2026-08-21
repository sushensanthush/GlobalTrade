<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="com.global.entity.*" %>
<%@ page import="java.util.*" %>

<%
    if (session == null || session.getAttribute("currentUser") == null) {
        response.sendRedirect(request.getContextPath() + "/login.jsp");
        return;
    }

    User currentUser = (User) session.getAttribute("currentUser");
    String ctx = request.getContextPath();

    Number totalShipmentsN = (Number) request.getAttribute("totalShipments");
    long totalShipments = totalShipmentsN != null ? totalShipmentsN.longValue() : 0L;

    Number delayedCountN = (Number) request.getAttribute("delayedCount");
    long delayedCount = delayedCountN != null ? delayedCountN.longValue() : 0L;

    Number inTransitCountN = (Number) request.getAttribute("inTransitCount");
    long inTransitCount = inTransitCountN != null ? inTransitCountN.longValue() : 0L;

    Number customsHoldCountN = (Number) request.getAttribute("customsHoldCount");
    long customsHoldCount = customsHoldCountN != null ? customsHoldCountN.longValue() : 0L;

    Number totalInventoryN = (Number) request.getAttribute("totalInventoryItems");
    int totalInventory = totalInventoryN != null ? totalInventoryN.intValue() : 0;

    Number lowStockCountN = (Number) request.getAttribute("lowStockCount");
    int lowStockCount = lowStockCountN != null ? lowStockCountN.intValue() : 0;

    Number totalVendorsN = (Number) request.getAttribute("totalVendors");
    long totalVendors = totalVendorsN != null ? totalVendorsN.longValue() : 0L;

    Number underperformingVendorsN = (Number) request.getAttribute("underperformingVendors");
    long underperformingVendors = underperformingVendorsN != null ? underperformingVendorsN.longValue() : 0L;

    @SuppressWarnings("unchecked")
    List<Shipment> recentShipments = (List<Shipment>) request.getAttribute("recentShipments");

    @SuppressWarnings("unchecked")
    List<InventoryItem> lowStockItems = (List<InventoryItem>) request.getAttribute("lowStockItems");

    @SuppressWarnings("unchecked")
    List<AuditLog> recentAuditLogs = (List<AuditLog>) request.getAttribute("recentAuditLogs");

    @SuppressWarnings("unchecked")
    List<TimerLog> recentTimerLogs = (List<TimerLog>) request.getAttribute("recentTimerLogs");

    String errorMsg = request.getParameter("error");
%>

<!DOCTYPE html>
<html lang="en">

<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Dashboard | GlobalTrade</title>
    <link rel="stylesheet" href="<%= ctx %>/css/style.css">

    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.1/css/all.min.css">
</head>

<body>

<div class="app-shell">

    <%@ include file="/WEB-INF/nav-include.jsp" %>

    <div class="main-content">


        <div class="topbar">
            <span class="topbar-title">
                <i class="fa-solid fa-chart-line" style="margin-right: 8px; color: #4f46e5;"></i> Operations Dashboard
            </span>

            <div class="topbar-actions">
                <a href="<%= ctx %>/DashboardServlet" class="btn btn-outline btn-sm">
                    <i class="fa-solid fa-rotate"></i> Refresh
                </a>
                <a href="<%= ctx %>/ShipmentServlet?action=list" class="btn btn-primary btn-sm">
                    <i class="fa-solid fa-plus"></i> New Shipment
                </a>
            </div>
        </div>

        <div class="page-body">


            <% if ("access_denied".equals(errorMsg)) { %>
            <div class="alert alert-danger">
                <i class="fa-solid fa-triangle-exclamation"></i> Access denied. You do not have permission to perform that operation.
            </div>
            <% } %>


            <div class="stats-grid">


                <div class="stat-card info">
                    <div class="stat-label">
                        <i class="fa-solid fa-truck-fast"></i> Total Shipments
                    </div>
                    <div class="stat-value"><%= totalShipments %></div>
                    <div class="stat-sub"><%= inTransitCount %> in transit</div>
                </div>

                <div class="stat-card <%= delayedCount > 0 ? "danger" : "success" %>">
                    <div class="stat-label">
                        <i class="fa-solid fa-clock-rotate-left"></i> Delayed Shipments
                    </div>
                    <div class="stat-value"><%= delayedCount %></div>
                    <div class="stat-sub">Requires immediate action</div>
                </div>


                <div class="stat-card <%= customsHoldCount > 0 ? "warning" : "success" %>">
                    <div class="stat-label">
                        <i class="fa-solid fa-building-shield"></i> Customs Hold
                    </div>
                    <div class="stat-value"><%= customsHoldCount %></div>
                    <div class="stat-sub">Awaiting clearance</div>
                </div>


                <div class="stat-card <%= lowStockCount > 0 ? "warning" : "success" %>">
                    <div class="stat-label">
                        <i class="fa-solid fa-boxes-stacked"></i> Low Stock Alerts
                    </div>
                    <div class="stat-value"><%= lowStockCount %></div>
                    <div class="stat-sub">of <%= totalInventory %> tracked items</div>
                </div>


                <div class="stat-card <%= underperformingVendors > 0 ? "warning" : "" %>">
                    <div class="stat-label">
                        <i class="fa-solid fa-user-slash"></i> Underperforming Vendors
                    </div>
                    <div class="stat-value"><%= underperformingVendors %></div>
                    <div class="stat-sub">of <%= totalVendors %> active vendors</div>
                </div>

            </div>


            <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 20px;">


                <div class="card">
                    <div class="card-header">
                        <span class="card-title">
                            <i class="fa-solid fa-ship" style="color: #4f46e5; margin-right: 6px;"></i> Recent Shipments
                        </span>
                        <a href="<%= ctx %>/ShipmentServlet?action=list" class="btn btn-outline btn-sm">View All</a>
                    </div>

                    <div class="card-body p-0">
                        <% if (recentShipments == null || recentShipments.isEmpty()) { %>
                        <div class="empty-state">
                            <p><i class="fa-regular fa-folder-open"></i> No shipments recorded.</p>
                        </div>
                        <% } else { %>
                        <table>
                            <thead>
                            <tr>
                                <th>Tracking</th>
                                <th>Route</th>
                                <th>Status</th>
                            </tr>
                            </thead>
                            <tbody>
                            <% for (Shipment s : recentShipments) { %>
                            <tr>
                                <td>
                                    <a href="<%= ctx %>/ShipmentServlet?action=view&id=<%= s.getId() %>" class="mono">
                                        <i class="fa-solid fa-barcode"></i> <%= s.getTrackingNumber() %>
                                    </a>
                                </td>
                                <td class="text-muted" style="font-size:12px;">
                                    <%= s.getOriginCountry() %> &rarr; <%= s.getDestinationCountry() %>
                                </td>
                                <td>
                                    <span class="badge status-<%= s.getStatus().name() %>">
                                        <%= s.getStatus().name().replace("_", " ") %>
                                    </span>
                                </td>
                            </tr>
                            <% } %>
                            </tbody>
                        </table>
                        <% } %>
                    </div>
                </div>


                <div class="card">
                    <div class="card-header">
                        <span class="card-title">
                            <i class="fa-solid fa-triangle-exclamation" style="color: #eab308; margin-right: 6px;"></i> Low Stock Alerts
                        </span>
                        <a href="<%= ctx %>/InventoryServlet" class="btn btn-outline btn-sm">View Inventory</a>
                    </div>

                    <div class="card-body p-0">
                        <% if (lowStockItems == null || lowStockItems.isEmpty()) { %>
                        <div class="empty-state">
                            <p><i class="fa-regular fa-circle-check" style="color: #22c55e;"></i> All inventory levels are adequate.</p>
                        </div>
                        <% } else { %>
                        <table>
                            <thead>
                            <tr>
                                <th>Item Code</th>
                                <th>Item Name</th>
                                <th>On Hand</th>
                                <th>Reorder Pt.</th>
                            </tr>
                            </thead>
                            <tbody>
                            <% for (InventoryItem item : lowStockItems) { %>
                            <tr>
                                <td class="mono"><%= item.getItemCode() %></td>
                                <td><%= item.getItemName() %></td>
                                <td class="text-danger fw-600"><%= item.getQuantityOnHand() %></td>
                                <td class="text-muted"><%= item.getReorderPoint() %></td>
                            </tr>
                            <% } %>
                            </tbody>
                        </table>
                        <% } %>
                    </div>
                </div>

            </div>


            <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 20px; margin-top: 20px;">


                <div class="card">
                    <div class="card-header">
                        <span class="card-title">
                            <i class="fa-solid fa-shield-halved" style="color: #4f46e5; margin-right: 6px;"></i> Recent Audit Events
                        </span>
                        <a href="<%= ctx %>/AuditServlet" class="btn btn-outline btn-sm">View All</a>
                    </div>

                    <div class="card-body p-0">
                        <% if (recentAuditLogs == null || recentAuditLogs.isEmpty()) { %>
                        <div class="empty-state">
                            <p>No audit events in the last 6 hours.</p>
                        </div>
                        <% } else { %>
                        <table>
                            <thead>
                            <tr>
                                <th>Action</th>
                                <th>User</th>
                                <th>Result</th>
                                <th>Time (ms)</th>
                            </tr>
                            </thead>
                            <tbody>
                            <% for (AuditLog log : recentAuditLogs) { %>
                            <tr>
                                <td style="font-size:12px;"><%= log.getAction() %></td>
                                <td class="text-muted" style="font-size:12px;">
                                    <%= log.getUsername() != null ? log.getUsername() : "—" %>
                                </td>
                                <td>
                                    <span class="badge <%= log.isSuccess() ? "badge-success" : "badge-danger" %>">
                                        <%= log.isSuccess() ? "OK" : "FAIL" %>
                                    </span>
                                </td>
                                <td class="text-muted mono">
                                    <%= log.getExecutionTimeMs() != null ? log.getExecutionTimeMs() : "—" %>
                                </td>
                            </tr>
                            <% } %>
                            </tbody>
                        </table>
                        <% } %>
                    </div>
                </div>


                <div class="card">
                    <div class="card-header">
                        <span class="card-title">
                            <i class="fa-solid fa-stopwatch" style="color: #4f46e5; margin-right: 6px;"></i> Timer Service Executions
                        </span>
                    </div>

                    <div class="card-body p-0">
                        <% if (recentTimerLogs == null || recentTimerLogs.isEmpty()) { %>
                        <div class="empty-state">
                            <p>No timer executions in the last 6 hours.</p>
                        </div>
                        <% } else { %>
                        <table>
                            <thead>
                            <tr>
                                <th>Timer</th>
                                <th>Records</th>
                                <th>Result</th>
                            </tr>
                            </thead>
                            <tbody>
                            <% for (TimerLog tl : recentTimerLogs) { %>
                            <tr>
                                <td style="font-size:12px;"><%= tl.getTimerType() %></td>
                                <td class="mono"><%= tl.getRecordsProcessed() %></td>
                                <td>
                                    <span class="badge <%= tl.isSuccess() ? "badge-success" : "badge-danger" %>">
                                        <%= tl.isSuccess() ? "OK" : "FAIL" %>
                                    </span>
                                </td>
                            </tr>
                            <% } %>
                            </tbody>
                        </table>
                        <% } %>
                    </div>
                </div>

            </div>

        </div>


    </div>


</div>


</body>
</html>