<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="com.global.entity.*" %>
<%@ page import="java.util.*" %>
<%
    if (session == null || session.getAttribute("currentUser") == null) {
        response.sendRedirect(request.getContextPath() + "/login.jsp"); return;
    }
    User currentUser = (User) session.getAttribute("currentUser");
    String ctx = request.getContextPath();
    boolean canManage = currentUser.getRole() == UserRole.LOGISTICS_COORDINATOR
            || currentUser.getRole() == UserRole.SYSTEM_ADMIN;
    @SuppressWarnings("unchecked")
    List<Vendor> vendors = (List<Vendor>) request.getAttribute("vendors");
    if (vendors == null) vendors = new ArrayList<>();
    String msg = request.getParameter("msg");
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Vendors | GlobalTrade</title>
    <link rel="stylesheet" href="<%= ctx %>/css/style.css">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.1/css/all.min.css">
</head>
<body>
<div class="app-shell">
    <%@ include file="/WEB-INF/nav-include.jsp" %>
    <div class="main-content">
        <div class="topbar">
            <span class="topbar-title">Vendor Management</span>
            <div class="topbar-actions">
                <% if (canManage) { %>
                <button class="btn btn-primary btn-sm" onclick="openModal('createVendorModal')">Add Vendor</button>
                <% } %>
            </div>
        </div>
        <div class="page-body">
            <% if ("created".equals(msg)) { %><div class="alert alert-success">Vendor created successfully.</div><% } %>
            <% if ("updated".equals(msg)) { %><div class="alert alert-success">Performance scores updated.</div><% } %>

            <div class="card">
                <div class="card-header">
                    <span class="card-title">Registered Vendors (<%= vendors.size() %>)</span>
                </div>
                <div class="card-body p-0">
                    <% if (vendors.isEmpty()) { %>
                    <div class="empty-state"><p>No vendors registered.</p></div>
                    <% } else { %>
                    <div class="table-container">
                    <table>
                        <thead>
                        <tr>
                            <th>Vendor Code</th>
                            <th>Name</th>
                            <th>Country</th>
                            <th>Email</th>
                            <th>Performance</th>
                            <th>OTD Rate</th>
                            <th>Status</th>
                            <% if (canManage) { %><th>Actions</th><% } %>
                        </tr>
                        </thead>
                        <tbody>
                        <% for (Vendor v : vendors) {
                            double score = v.getPerformanceScore() != null ? v.getPerformanceScore().doubleValue() : 0;
                            String scoreBadge = score >= 90 ? "badge-success" : score >= 75 ? "badge-warning" : "badge-danger";
                        %>
                        <tr>
                            <td class="mono"><%= v.getVendorCode() %></td>
                            <td class="fw-600"><%= v.getName() %></td>
                            <td><%= v.getCountryCode() %></td>
                            <td class="text-muted" style="font-size:12px;"><%= v.getContactEmail() != null ? v.getContactEmail() : "—" %></td>
                            <td><span class="badge <%= scoreBadge %>"><%= v.getPerformanceScore() != null ? v.getPerformanceScore().toPlainString() : "—" %></span></td>
                            <td class="text-muted"><%= v.getOnTimeDeliveryRate() != null ? v.getOnTimeDeliveryRate().toPlainString() + "%" : "—" %></td>
                            <td>
                                <span class="badge <%= v.isActive() ? "badge-success" : "badge-secondary" %>">
                                    <%= v.isActive() ? "Active" : "Inactive" %>
                                </span>
                            </td>
                            <% if (canManage) { %>
                            <td>
                                <button class="btn btn-outline btn-sm"
                                        onclick="openScoreModal(<%= v.getId() %>, '<%= v.getName() %>',
                                        <%= v.getPerformanceScore() != null ? v.getPerformanceScore() : 100 %>,
                                        <%= v.getOnTimeDeliveryRate() != null ? v.getOnTimeDeliveryRate() : 100 %>)">
                                    Update Score
                                </button>
                            </td>
                            <% } %>
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


<% if (canManage) { %>
<div class="modal-backdrop" id="createVendorModal">
    <div class="modal">
        <div class="modal-header">
            <span class="modal-title">Register New Vendor</span>
            <button class="modal-close" onclick="closeModal('createVendorModal')">&times;</button>
        </div>
        <div class="modal-body">
            <form action="<%= ctx %>/VendorServlet" method="POST" id="createVendorForm">
                <input type="hidden" name="action" value="create">
                <div class="form-group">
                    <label class="form-label">Vendor Name <span class="required">*</span></label>
                    <input type="text" name="name" class="form-control" required placeholder="e.g. AcmeCorp Logistics">
                </div>
                <div class="form-row">
                    <div class="form-group">
                        <label class="form-label">Country Code <span class="required">*</span></label>
                        <input type="text" name="countryCode" class="form-control" required
                               placeholder="e.g. US" maxlength="10">
                    </div>
                    <div class="form-group">
                        <label class="form-label">Contact Phone</label>
                        <input type="text" name="contactPhone" class="form-control" placeholder="+1-555-0100">
                    </div>
                </div>
                <div class="form-group">
                    <label class="form-label">Contact Email <span class="required">*</span></label>
                    <input type="email" name="contactEmail" class="form-control" required
                           placeholder="contact@vendor.com">
                </div>
            </form>
        </div>
        <div class="modal-footer">
            <button type="button" class="btn btn-outline" onclick="closeModal('createVendorModal')">Cancel</button>
            <button type="submit" form="createVendorForm" class="btn btn-primary">Register Vendor</button>
        </div>
    </div>
</div>


<div class="modal-backdrop" id="scoreModal">
    <div class="modal">
        <div class="modal-header">
            <span class="modal-title">Update Performance Score</span>
            <button class="modal-close" onclick="closeModal('scoreModal')">&times;</button>
        </div>
        <div class="modal-body">
            <p id="scoreVendorName" style="font-weight:600; margin-bottom:16px;"></p>
            <form action="<%= ctx %>/VendorServlet" method="POST" id="scoreForm">
                <input type="hidden" name="action" value="updateScore">
                <input type="hidden" name="vendorId" id="scoreVendorId">
                <div class="form-row">
                    <div class="form-group">
                        <label class="form-label">Performance Score (0–100)</label>
                        <input type="number" name="performanceScore" id="scoreInput"
                               class="form-control" min="0" max="100" step="0.1" required>
                    </div>
                    <div class="form-group">
                        <label class="form-label">OTD Rate % (0–100)</label>
                        <input type="number" name="onTimeDeliveryRate" id="otdInput"
                               class="form-control" min="0" max="100" step="0.1" required>
                    </div>
                </div>
            </form>
        </div>
        <div class="modal-footer">
            <button type="button" class="btn btn-outline" onclick="closeModal('scoreModal')">Cancel</button>
            <button type="submit" form="scoreForm" class="btn btn-accent">Save Scores</button>
        </div>
    </div>
</div>
<% } %>

<script>
    function openModal(id) { document.getElementById(id).classList.add('open'); }
    function closeModal(id) { document.getElementById(id).classList.remove('open'); }
    function openScoreModal(id, name, score, otd) {
        document.getElementById('scoreVendorId').value = id;
        document.getElementById('scoreVendorName').textContent = name;
        document.getElementById('scoreInput').value = score;
        document.getElementById('otdInput').value = otd;
        openModal('scoreModal');
    }
    document.querySelectorAll('.modal-backdrop').forEach(m => {
        m.addEventListener('click', e => { if (e.target === m) m.classList.remove('open'); });
    });
</script>
</body>
</html>
