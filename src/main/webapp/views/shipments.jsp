<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="com.global.entity.*" %>
<%@ page import="java.util.*" %>
<%
    if (session == null || session.getAttribute("currentUser") == null) {
        response.sendRedirect(request.getContextPath() + "/login.jsp"); return;
    }
    User currentUser = (User) session.getAttribute("currentUser");
    String ctx = request.getContextPath();
    boolean canCreate = currentUser.getRole() == UserRole.LOGISTICS_COORDINATOR
            || currentUser.getRole() == UserRole.SYSTEM_ADMIN;

    @SuppressWarnings("unchecked")
    List<Shipment> shipments = (List<Shipment>) request.getAttribute("shipments");
    if (shipments == null) shipments = new ArrayList<>();

    String msg = request.getParameter("msg");
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Shipments | GlobalTrade</title>
    <link rel="stylesheet" href="<%= ctx %>/css/style.css">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.1/css/all.min.css">
</head>
<body>
<div class="app-shell">
    <%@ include file="/WEB-INF/nav-include.jsp" %>
    <div class="main-content">
        <div class="topbar">
            <span class="topbar-title">Shipment Management</span>
            <div class="topbar-actions">
                <% if (canCreate) { %>
                <button class="btn btn-primary btn-sm" onclick="openModal('createModal')">Create Shipment</button>
                <% } %>
            </div>
        </div>
        <div class="page-body">
            <% if ("created".equals(msg)) { %><div class="alert alert-success">Shipment created successfully.</div><% } %>
            <% if ("cancelled".equals(msg)) { %><div class="alert alert-warning">Shipment cancelled.</div><% } %>

            <div class="card">
                <div class="card-header">
                    <span class="card-title">All Shipments (<%= shipments.size() %>)</span>
                </div>
                <div class="card-body p-0">
                    <% if (shipments.isEmpty()) { %>
                    <div class="empty-state"><p>No shipments found. Create the first shipment to get started.</p></div>
                    <% } else { %>
                    <div class="table-container">
                    <table>
                        <thead>
                        <tr>
                            <th>Tracking Number</th>
                            <th>Origin</th>
                            <th>Destination</th>
                            <th>Carrier</th>
                            <th>Status</th>
                            <th>Customs</th>
                            <th>Est. Delivery</th>
                            <th>Actions</th>
                        </tr>
                        </thead>
                        <tbody>
                        <% for (Shipment s : shipments) { %>
                        <tr>
                            <td>
                                <a href="<%= ctx %>/ShipmentServlet?action=view&id=<%= s.getId() %>" class="mono fw-600">
                                    <%= s.getTrackingNumber() %>
                                </a>
                            </td>
                            <td><%= s.getOriginCountry() %></td>
                            <td><%= s.getDestinationCountry() %></td>
                            <td class="text-muted"><%= s.getCarrierName() != null ? s.getCarrierName() : "—" %></td>
                            <td>
                                <span class="badge status-<%= s.getStatus().name() %>">
                                    <%= s.getStatus().name().replace("_", " ") %>
                                </span>
                            </td>
                            <td>
                                <span class="badge <%= s.isCustomsCleared() ? "badge-success" : "badge-secondary" %>">
                                    <%= s.isCustomsCleared() ? "Cleared" : "Pending" %>
                                </span>
                            </td>
                            <td class="text-muted" style="font-size:12px;">
                                <%= s.getEstimatedDelivery() != null
                                        ? s.getEstimatedDelivery().toLocalDate().toString() : "—" %>
                            </td>
                            <td>
                                <a href="<%= ctx %>/ShipmentServlet?action=view&id=<%= s.getId() %>"
                                   class="btn btn-outline btn-sm">View</a>
                                <% if (canCreate && s.getStatus() != ShipmentStatus.CANCELLED
                                        && s.getStatus() != ShipmentStatus.DELIVERED) { %>
                                <a href="<%= ctx %>/ShipmentServlet?action=delete&id=<%= s.getId() %>"
                                   class="btn btn-danger btn-sm"
                                   onclick="return confirm('Cancel shipment <%= s.getTrackingNumber() %>?')">Cancel</a>
                                <% } %>
                            </td>
                        </tr>
                        <% } %>
                        </tbody>
                    </table>
                    </div>
                    <% } %>
                </div>
            </div>
        </div>
    </div>
</div>

<% if (canCreate) { %>
<div class="modal-backdrop" id="createModal">
    <div class="modal">
        <div class="modal-header">
            <span class="modal-title">Create New Shipment</span>
            <button class="modal-close" onclick="closeModal('createModal')">&times;</button>
        </div>
        <div class="modal-body">
            <form action="<%= ctx %>/ShipmentServlet" method="POST" id="createForm">
                <input type="hidden" name="action" value="create">
                <div class="form-row">
                    <div class="form-group">
                        <label class="form-label">Origin Country <span class="required">*</span></label>
                        <input type="text" name="originCountry" class="form-control" required placeholder="e.g. China">
                    </div>
                    <div class="form-group">
                        <label class="form-label">Destination Country <span class="required">*</span></label>
                        <input type="text" name="destinationCountry" class="form-control" required placeholder="e.g. United States">
                    </div>
                </div>
                <div class="form-row">
                    <div class="form-group">
                        <label class="form-label">Carrier Name</label>
                        <input type="text" name="carrierName" class="form-control" placeholder="e.g. GlobalShip Express">
                    </div>
                    <div class="form-group">
                        <label class="form-label">Est. Delivery Days</label>
                        <input type="number" name="estimatedDays" class="form-control" value="14" min="1" max="365">
                    </div>
                </div>
                <div class="form-row">
                    <div class="form-group">
                        <label class="form-label">Weight (kg)</label>
                        <input type="number" name="weightKg" class="form-control" step="0.001" placeholder="0.000">
                    </div>
                    <div class="form-group">
                        <label class="form-label">Declared Value <span class="required">*</span></label>
                        <input type="number" name="declaredValue" class="form-control" step="0.01" required placeholder="0.00">
                    </div>
                    <div class="form-group">
                        <label class="form-label">Currency</label>
                        <select name="currencyCode" class="form-control">
                            <option value="USD">USD</option>
                            <option value="EUR">EUR</option>
                            <option value="GBP">GBP</option>
                            <option value="JPY">JPY</option>
                            <option value="CNY">CNY</option>
                        </select>
                    </div>
                </div>
                <div class="form-group">
                    <label class="form-label">Description</label>
                    <textarea name="description" class="form-control" placeholder="Describe the shipment contents..."></textarea>
                </div>
            </form>
        </div>
        <div class="modal-footer">
            <button type="button" class="btn btn-outline" onclick="closeModal('createModal')">Cancel</button>
            <button type="submit" form="createForm" class="btn btn-primary">Create Shipment</button>
        </div>
    </div>
</div>
<% } %>

<script>
    function openModal(id) { document.getElementById(id).classList.add('open'); }
    function closeModal(id) { document.getElementById(id).classList.remove('open'); }
    document.querySelectorAll('.modal-backdrop').forEach(m => {
        m.addEventListener('click', e => { if (e.target === m) m.classList.remove('open'); });
    });
</script>
</body>
</html>
