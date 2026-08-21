<%@ page import="com.global.entity.User" %>
<%@ page import="com.global.entity.UserRole" %>

<%
    User navUser = (User) session.getAttribute("currentUser");

    String navUsername = navUser != null && navUser.getUsername() != null ? navUser.getUsername() : "default";
    String navFullName = navUser != null && navUser.getFullName() != null ? navUser.getFullName() : "User";
    UserRole navRole = navUser != null ? navUser.getRole() : null;

    String avatarUrl = "https://api.dicebear.com/7.x/lorelei/svg?seed=" + navUsername;

    String navRoleLabel = navRole != null ? navRole.name().replace("_", " ") : "";
    String navCtx = request.getContextPath();
    String currentURI = request.getRequestURI();
%>

<div class="sidebar">

    <div class="sidebar-header">
        <span class="sidebar-brand">
            GlobalTrade
        </span>
        <span class="sidebar-brand-sub">Supply Chain Platform</span>
    </div>

    <div class="sidebar-user">
        <div class="sidebar-user-avatar" style="background-color: #ffffff; padding: 2px; overflow: hidden; display: flex; align-items: center; justify-content: center;">
            <img src="<%= avatarUrl %>" alt="User Avatar" style="width: 100%; height: 100%; border-radius: 50%; object-fit: cover;" />
        </div>

        <div class="sidebar-user-info">
            <span class="sidebar-user-name"><%= navFullName %></span>
            <span class="sidebar-user-role"><%= navRoleLabel %></span>
        </div>
    </div>

    <nav class="sidebar-nav">
        <span class="sidebar-nav-label">Main</span>
        <a href="<%= navCtx %>/DashboardServlet" class="<%= currentURI.contains("DashboardServlet") || currentURI.endsWith("dashboard.jsp") ? "active" : "" %>">
            <span class="nav-icon"><i class="fa-solid fa-chart-pie"></i></span> Dashboard
        </a>
        <a href="<%= navCtx %>/ShipmentServlet?action=list" class="<%= currentURI.contains("ShipmentServlet") || currentURI.contains("shipment") ? "active" : "" %>">
            <span class="nav-icon"><i class="fa-solid fa-truck"></i></span> Shipments
        </a>
        <a href="<%= navCtx %>/InventoryServlet" class="<%= currentURI.contains("InventoryServlet") || currentURI.contains("inventory") ? "active" : "" %>">
            <span class="nav-icon"><i class="fa-solid fa-warehouse"></i></span> Inventory
        </a>
        <a href="<%= navCtx %>/VendorServlet?action=list" class="<%= currentURI.contains("VendorServlet") || currentURI.contains("vendor") ? "active" : "" %>">
            <span class="nav-icon"><i class="fa-solid fa-handshake"></i></span> Vendors
        </a>

        <span class="sidebar-nav-label">Compliance</span>
        <a href="<%= navCtx %>/AuditServlet" class="<%= currentURI.contains("AuditServlet") || currentURI.contains("audit") ? "active" : "" %>">
            <span class="nav-icon"><i class="fa-solid fa-clipboard-list"></i></span> Audit Trail
        </a>
        <% if (navRole == UserRole.SYSTEM_ADMIN || navRole == UserRole.LOGISTICS_COORDINATOR) { %>
        <a href="<%= navCtx %>/AuditServlet?hours=1">
            <span class="nav-icon"><i class="fa-solid fa-clock"></i></span> Timer Logs
        </a>
        <% } %>
    </nav>

    <div class="sidebar-footer">
        <a href="<%= navCtx %>/LogoutServlet">
            <span><i class="fa-solid fa-right-from-bracket"></i></span> Sign Out
        </a>
    </div>

</div>