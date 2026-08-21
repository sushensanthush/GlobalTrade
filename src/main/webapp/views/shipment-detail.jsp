<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="com.global.entity.*" %>
<%
    if (session == null || session.getAttribute("currentUser") == null) {
        response.sendRedirect(request.getContextPath() + "/login.jsp"); return;
    }
    User currentUser = (User) session.getAttribute("currentUser");
    String ctx = request.getContextPath();
    Shipment shipment = (Shipment) request.getAttribute("shipment");
    if (shipment == null) {
        response.sendRedirect(ctx + "/ShipmentServlet?action=list"); return;
    }

    boolean isLogistics = currentUser.getRole() == UserRole.LOGISTICS_COORDINATOR
            || currentUser.getRole() == UserRole.SYSTEM_ADMIN;
    boolean isCustoms = currentUser.getRole() == UserRole.CUSTOMS_AGENT
            || currentUser.getRole() == UserRole.SYSTEM_ADMIN;
    String msg = request.getParameter("msg");
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Shipment <%= shipment.getTrackingNumber() %>  | GlobalTrade</title>
    <link rel="stylesheet" href="<%= ctx %>/css/style.css">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.1/css/all.min.css">
</head>
<body>
<div class="app-shell">
    <%@ include file="/WEB-INF/nav-include.jsp" %>
    <div class="main-content">
        <div class="topbar">
            <span class="topbar-title">Shipment Detail</span>
            <div class="topbar-actions">
                <a href="<%= ctx %>/ShipmentServlet?action=list" class="btn btn-outline btn-sm">Back to List</a>
            </div>
        </div>
        <div class="page-body">
            <div class="breadcrumb">
                <a href="<%= ctx %>/DashboardServlet">Dashboard</a>
                <span class="sep">/</span>
                <a href="<%= ctx %>/ShipmentServlet?action=list">Shipments</a>
                <span class="sep">/</span>
                <span><%= shipment.getTrackingNumber() %></span>
            </div>

            <% if ("updated".equals(msg)) { %><div class="alert alert-success">Status updated successfully.</div><% } %>
            <% if ("customs_cleared".equals(msg)) { %><div class="alert alert-success">Customs clearance processed successfully.</div><% } %>
            <% if ("created".equals(msg)) { %><div class="alert alert-success">Shipment created successfully.</div><% } %>

            <div style="display:grid; grid-template-columns: 2fr 1fr; gap: 20px;">
                <!-- Details -->
                <div>
                    <div class="card">
                        <div class="card-header">
                            <span class="card-title">Shipment Information</span>
                            <span class="badge status-<%= shipment.getStatus().name() %>">
                                <%= shipment.getStatus().name().replace("_", " ") %>
                            </span>
                        </div>
                        <div class="card-body">
                            <div class="form-row">
                                <div>
                                    <div class="form-label text-muted" style="font-size:11px; text-transform:uppercase; letter-spacing:0.6px;">Tracking Number</div>
                                    <div class="mono fw-600" style="font-size:16px;"><%= shipment.getTrackingNumber() %></div>
                                </div>
                                <div>
                                    <div class="form-label text-muted" style="font-size:11px; text-transform:uppercase; letter-spacing:0.6px;">Carrier</div>
                                    <div><%= shipment.getCarrierName() != null ? shipment.getCarrierName() : "—" %></div>
                                </div>
                            </div>
                            <hr style="border:none; border-top:1px solid var(--border-light); margin:16px 0;">
                            <div class="form-row">
                                <div>
                                    <div class="form-label text-muted" style="font-size:11px; text-transform:uppercase; letter-spacing:0.6px;">Origin</div>
                                    <div><%= shipment.getOriginCountry() %></div>
                                </div>
                                <div>
                                    <div class="form-label text-muted" style="font-size:11px; text-transform:uppercase; letter-spacing:0.6px;">Destination</div>
                                    <div><%= shipment.getDestinationCountry() %></div>
                                </div>
                            </div>
                            <div class="form-row" style="margin-top:16px;">
                                <div>
                                    <div class="form-label text-muted" style="font-size:11px; text-transform:uppercase; letter-spacing:0.6px;">Weight (kg)</div>
                                    <div><%= shipment.getWeightKg() != null ? shipment.getWeightKg().toPlainString() : "—" %></div>
                                </div>
                                <div>
                                    <div class="form-label text-muted" style="font-size:11px; text-transform:uppercase; letter-spacing:0.6px;">Declared Value</div>
                                    <div class="fw-600"><%= shipment.getDeclaredValue() != null ? shipment.getDeclaredValue().toPlainString() : "0" %> <%= shipment.getCurrencyCode() != null ? shipment.getCurrencyCode() : "" %></div>
                                </div>
                                <div>
                                    <div class="form-label text-muted" style="font-size:11px; text-transform:uppercase; letter-spacing:0.6px;">Customs Status</div>
                                    <div>
                                        <span class="badge <%= shipment.isCustomsCleared() ? "badge-success" : "badge-secondary" %>">
                                            <%= shipment.isCustomsCleared() ? "Cleared" : "Not Cleared" %>
                                        </span>
                                    </div>
                                </div>
                            </div>
                            <% if (shipment.getCustomsDeclarationNumber() != null) { %>
                            <div style="margin-top:16px;">
                                <div class="form-label text-muted" style="font-size:11px; text-transform:uppercase; letter-spacing:0.6px;">Customs Declaration No.</div>
                                <div class="mono"><%= shipment.getCustomsDeclarationNumber() %></div>
                            </div>
                            <% } %>
                            <div class="form-row" style="margin-top:16px;">
                                <div>
                                    <div class="form-label text-muted" style="font-size:11px; text-transform:uppercase; letter-spacing:0.6px;">Est. Delivery</div>
                                    <div><%= shipment.getEstimatedDelivery() != null ? shipment.getEstimatedDelivery().toLocalDate() : "—" %></div>
                                </div>
                                <div>
                                    <div class="form-label text-muted" style="font-size:11px; text-transform:uppercase; letter-spacing:0.6px;">Actual Delivery</div>
                                    <div><%= shipment.getActualDelivery() != null ? shipment.getActualDelivery().toLocalDate() : "—" %></div>
                                </div>
                            </div>
                            <% if (shipment.getDescription() != null) { %>
                            <div style="margin-top:16px;">
                                <div class="form-label text-muted" style="font-size:11px; text-transform:uppercase; letter-spacing:0.6px;">Description</div>
                                <div style="font-size:13px; color:var(--text-secondary);"><%= shipment.getDescription() %></div>
                            </div>
                            <% } %>
                        </div>
                    </div>
                </div>


                <div>
                    <% if (isLogistics && shipment.getStatus() != ShipmentStatus.DELIVERED
                            && shipment.getStatus() != ShipmentStatus.CANCELLED) { %>
                    <div class="card">
                        <div class="card-header"><span class="card-title">Update Status</span></div>
                        <div class="card-body">
                            <form action="<%= ctx %>/ShipmentServlet" method="POST">
                                <input type="hidden" name="action" value="updateStatus">
                                <input type="hidden" name="id" value="<%= shipment.getId() %>">
                                <div class="form-group">
                                    <label class="form-label">New Status</label>
                                    <select name="status" class="form-control">
                                        <% for (ShipmentStatus st : ShipmentStatus.values()) { %>
                                        <option value="<%= st.name() %>" <%= st == shipment.getStatus() ? "selected" : "" %>>
                                            <%= st.name().replace("_", " ") %>
                                        </option>
                                        <% } %>
                                    </select>
                                </div>
                                <button type="submit" class="btn btn-primary" style="width:100%;">Update Status</button>
                            </form>
                        </div>
                    </div>
                    <% } %>

                    <% if (isCustoms && !shipment.isCustomsCleared()
                            && shipment.getStatus() != ShipmentStatus.CANCELLED) { %>
                    <div class="card">
                        <div class="card-header"><span class="card-title">Customs Clearance</span></div>
                        <div class="card-body">
                            <form action="<%= ctx %>/ShipmentServlet" method="POST">
                                <input type="hidden" name="action" value="clearCustoms">
                                <input type="hidden" name="id" value="<%= shipment.getId() %>">
                                <div class="form-group">
                                    <label class="form-label">Declaration Number <span class="required">*</span></label>
                                    <input type="text" name="declarationNumber" class="form-control"
                                           required placeholder="e.g. CUST-2026-00123">
                                    <div class="form-hint">Official customs declaration reference</div>
                                </div>
                                <button type="submit" class="btn btn-success" style="width:100%;">Process Clearance</button>
                            </form>
                        </div>
                    </div>
                    <% } %>

                    <div class="card">
                        <div class="card-header"><span class="card-title">Shipment Metadata</span></div>
                        <div class="card-body">
                            <div style="font-size:12px; color:var(--text-muted);">
                                <div style="margin-bottom:8px;">
                                    <strong>Created:</strong><br>
                                    <%= shipment.getCreatedAt() != null ? shipment.getCreatedAt().toString().replace("T", " ").substring(0, 16) : "—" %>
                                </div>
                                <div>
                                    <strong>Last Updated:</strong><br>
                                    <%= shipment.getUpdatedAt() != null ? shipment.getUpdatedAt().toString().replace("T", " ").substring(0, 16) : "—" %>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>
</body>
</html>
