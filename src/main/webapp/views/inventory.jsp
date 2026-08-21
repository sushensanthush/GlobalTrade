<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="com.global.entity.*" %>
<%@ page import="java.util.*" %>
<%
    if (session == null || session.getAttribute("currentUser") == null) {
        response.sendRedirect(request.getContextPath() + "/login.jsp"); return;
    }
    User currentUser = (User) session.getAttribute("currentUser");
    String ctx = request.getContextPath();
    boolean isWarehouse = currentUser.getRole() == UserRole.WAREHOUSE_MANAGER
            || currentUser.getRole() == UserRole.SYSTEM_ADMIN;
    boolean canDeduct = isWarehouse || currentUser.getRole() == UserRole.LOGISTICS_COORDINATOR;

    @SuppressWarnings("unchecked")
    List<InventoryItem> items = (List<InventoryItem>) request.getAttribute("items");
    @SuppressWarnings("unchecked")
    List<InventoryItem> lowStockItems = (List<InventoryItem>) request.getAttribute("lowStockItems");
    if (items == null) items = new ArrayList<>();
    if (lowStockItems == null) lowStockItems = new ArrayList<>();
    String msg = request.getParameter("msg");
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Inventory | GlobalTrade</title>
    <link rel="stylesheet" href="<%= ctx %>/css/style.css">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.1/css/all.min.css">
</head>
<body>
<div class="app-shell">
    <%@ include file="/WEB-INF/nav-include.jsp" %>
    <div class="main-content">
        <div class="topbar">
            <span class="topbar-title">Inventory Management</span>
            <div class="topbar-actions">
                <% if (isWarehouse) { %>
                <button class="btn btn-primary btn-sm" onclick="openModal('createItemModal')">Add Item</button>
                <% } %>
            </div>
        </div>
        <div class="page-body">
            <% if ("created".equals(msg)) { %><div class="alert alert-success">Inventory item created successfully.</div><% } %>
            <% if ("deducted".equals(msg)) { %><div class="alert alert-success">Stock deducted successfully.</div><% } %>
            <% if ("replenished".equals(msg)) { %><div class="alert alert-success">Stock replenished successfully.</div><% } %>

            <% if (!lowStockItems.isEmpty()) { %>
            <div class="alert alert-warning">
                <strong><%= lowStockItems.size() %> item(s) at or below reorder point.</strong>
                Replenishment action required.
            </div>
            <% } %>

            <div class="card">
                <div class="card-header">
                    <span class="card-title">All Inventory Items (<%= items.size() %>)</span>
                </div>
                <div class="card-body p-0">
                    <% if (items.isEmpty()) { %>
                    <div class="empty-state"><p>No inventory items recorded.</p></div>
                    <% } else { %>
                    <div class="table-container">
                    <table>
                        <thead>
                        <tr>
                            <th>Item Code</th>
                            <th>Name</th>
                            <th>On Hand</th>
                            <th>Reorder Pt.</th>
                            <th>Reorder Qty</th>
                            <th>Unit Cost</th>
                            <th>Warehouse</th>
                            <th>HS Code</th>
                            <th>Status</th>
                            <% if (canDeduct) { %><th>Actions</th><% } %>
                        </tr>
                        </thead>
                        <tbody>
                        <% for (InventoryItem item : items) {
                            boolean isLow = item.getQuantityOnHand() <= item.getReorderPoint();
                        %>
                        <tr>
                            <td class="mono"><%= item.getItemCode() %></td>
                            <td class="fw-600"><%= item.getItemName() %></td>
                            <td class="<%= isLow ? "text-danger fw-600" : "" %>"><%= item.getQuantityOnHand() %></td>
                            <td class="text-muted"><%= item.getReorderPoint() %></td>
                            <td class="text-muted"><%= item.getReorderQuantity() %></td>
                            <td class="text-muted">
                                <%= item.getUnitCost() != null ? item.getUnitCost().toPlainString() : "—" %>
                                <%= item.getCurrencyCode() != null ? item.getCurrencyCode() : "" %>
                            </td>
                            <td class="text-muted"><%= item.getWarehouseLocation() != null ? item.getWarehouseLocation() : "—" %></td>
                            <td class="mono text-muted" style="font-size:11px;"><%= item.getHsCode() != null ? item.getHsCode() : "—" %></td>
                            <td>
                                <span class="badge <%= isLow ? "badge-warning" : "badge-success" %>">
                                    <%= isLow ? "Low Stock" : "Adequate" %>
                                </span>
                            </td>
                            <% if (canDeduct) { %>
                            <td style="white-space:nowrap;">
                                <button class="btn btn-outline btn-sm"
                                        onclick="openStockModal('deduct', '<%= item.getItemCode() %>', '<%= item.getItemName() %>')">
                                    Deduct
                                </button>
                                <% if (isWarehouse) { %>
                                <button class="btn btn-success btn-sm"
                                        onclick="openStockModal('replenish', '<%= item.getItemCode() %>', '<%= item.getItemName() %>')">
                                    Replenish
                                </button>
                                <% } %>
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


<div class="modal-backdrop" id="stockModal">
    <div class="modal">
        <div class="modal-header">
            <span class="modal-title" id="stockModalTitle">Stock Operation</span>
            <button class="modal-close" onclick="closeModal('stockModal')">&times;</button>
        </div>
        <div class="modal-body">
            <p id="stockItemName" style="font-weight:600; margin-bottom:16px;"></p>
            <form action="<%= ctx %>/InventoryServlet" method="POST" id="stockForm">
                <input type="hidden" name="action" id="stockAction">
                <input type="hidden" name="itemCode" id="stockItemCode">
                <div class="form-group">
                    <label class="form-label">Quantity <span class="required">*</span></label>
                    <input type="number" name="quantity" class="form-control" required min="1" placeholder="Enter quantity">
                </div>
            </form>
        </div>
        <div class="modal-footer">
            <button type="button" class="btn btn-outline" onclick="closeModal('stockModal')">Cancel</button>
            <button type="submit" form="stockForm" class="btn btn-primary" id="stockSubmitBtn">Confirm</button>
        </div>
    </div>
</div>


<% if (isWarehouse) { %>
<div class="modal-backdrop" id="createItemModal">
    <div class="modal">
        <div class="modal-header">
            <span class="modal-title">Add Inventory Item</span>
            <button class="modal-close" onclick="closeModal('createItemModal')">&times;</button>
        </div>
        <div class="modal-body">
            <form action="<%= ctx %>/InventoryServlet" method="POST" id="createItemForm">
                <input type="hidden" name="action" value="create">
                <div class="form-row">
                    <div class="form-group">
                        <label class="form-label">Item Code <span class="required">*</span></label>
                        <input type="text" name="itemCode" class="form-control" required placeholder="e.g. ELC-005">
                    </div>
                    <div class="form-group">
                        <label class="form-label">Item Name <span class="required">*</span></label>
                        <input type="text" name="itemName" class="form-control" required placeholder="Item description">
                    </div>
                </div>
                <div class="form-row">
                    <div class="form-group">
                        <label class="form-label">Initial Quantity</label>
                        <input type="number" name="quantityOnHand" class="form-control" value="0" min="0">
                    </div>
                    <div class="form-group">
                        <label class="form-label">Reorder Point</label>
                        <input type="number" name="reorderPoint" class="form-control" value="50" min="0">
                    </div>
                    <div class="form-group">
                        <label class="form-label">Reorder Quantity</label>
                        <input type="number" name="reorderQuantity" class="form-control" value="100" min="1">
                    </div>
                </div>
                <div class="form-row">
                    <div class="form-group">
                        <label class="form-label">Unit Cost</label>
                        <input type="number" name="unitCost" class="form-control" step="0.01" placeholder="0.00">
                    </div>
                    <div class="form-group">
                        <label class="form-label">Currency</label>
                        <select name="currencyCode" class="form-control">
                            <option value="USD">USD</option>
                            <option value="EUR">EUR</option>
                            <option value="GBP">GBP</option>
                        </select>
                    </div>
                </div>
                <div class="form-row">
                    <div class="form-group">
                        <label class="form-label">Warehouse Location</label>
                        <input type="text" name="warehouseLocation" class="form-control" placeholder="e.g. Warehouse-A">
                    </div>
                    <div class="form-group">
                        <label class="form-label">HS Code</label>
                        <input type="text" name="hsCode" class="form-control" placeholder="e.g. 8534.00">
                    </div>
                </div>
            </form>
        </div>
        <div class="modal-footer">
            <button type="button" class="btn btn-outline" onclick="closeModal('createItemModal')">Cancel</button>
            <button type="submit" form="createItemForm" class="btn btn-primary">Add Item</button>
        </div>
    </div>
</div>
<% } %>

<script>
    function openModal(id) { document.getElementById(id).classList.add('open'); }
    function closeModal(id) { document.getElementById(id).classList.remove('open'); }
    function openStockModal(action, code, name) {
        document.getElementById('stockAction').value = action;
        document.getElementById('stockItemCode').value = code;
        document.getElementById('stockItemName').textContent = name;
        document.getElementById('stockModalTitle').textContent =
            action === 'deduct' ? 'Deduct Stock' : 'Replenish Stock';
        document.getElementById('stockSubmitBtn').textContent =
            action === 'deduct' ? 'Deduct Stock' : 'Replenish Stock';
        openModal('stockModal');
    }
    document.querySelectorAll('.modal-backdrop').forEach(m => {
        m.addEventListener('click', e => { if (e.target === m) m.classList.remove('open'); });
    });
</script>
</body>
</html>
