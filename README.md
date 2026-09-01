# GlobalTrade Supply Chain Management System
<p align="justify">
  <img src="https://img.shields.io/badge/Java-ED8B00?logo=openjdk&logoColor=white"/>
  <img src="https://img.shields.io/badge/MySQL-4479A1?logo=mysql&logoColor=white"/>
  <img src="https://img.shields.io/badge/Java%20Swing-UI%20Framework-007396?logo=java&logoColor=white"/>
  <img src="https://img.shields.io/badge/FlatLaf-3.1.1-6C3483?logoColor=white"/>
  <img src="https://img.shields.io/badge/JasperReports-Reporting-2E86C1?logoColor=white"/>
  <img src="https://img.shields.io/badge/Apache%20Ant-Build-A81C7D?logo=apache&logoColor=white"/>
  <img src="https://img.shields.io/badge/NetBeans-IDE-1B6AC6?logo=apachenetbeanside&logoColor=white"/>
</p>
A Jakarta EE 10 enterprise application implementing automated supply chain operations for multinational logistics across a Maven multi-module EAR architecture, deployed on GlassFish 7 with MySQL 8.0.

---

## Technology Stack

| Layer | Technology |
|---|---|
| Platform | Jakarta EE 10 (EJB 4.0, JPA 3.1, JAX-RS 3.1, Servlet 6) |
| Application Server | GlassFish 7.0.21 |
| Database | MySQL 8.0 |
| Persistence | EclipseLink (shared entity cache, JDBC batch write size 50) |
| Build | Maven 3 — multi-module (EJB JAR + WAR + EAR) |
| Java | JDK 17 |
| Testing | JUnit 5, Mockito 5, Arquillian 1.10, JMeter 5.6.3, Postman |
| Security | JAAS custom LoginModule, SHA-256, three-layer RBAC |

---

## Project Structure

```
GlobalTrade/
├── globaltrade-ejb/          # EJB JAR — business logic, timers, interceptors, security
│   └── src/main/java/com/global/
│       ├── entity/           # JPA entities (Shipment, Vendor, InventoryItem, AuditLog, TimerLog, User)
│       ├── session/          # Stateless EJBs (ShipmentMgmt, VendorMgmt, InventoryMgmt, Auth, Audit)
│       ├── timer/            # SupplyChainTimerService (@Singleton @Startup)
│       ├── interceptor/      # ValidationInterceptor, PerformanceInterceptor, AuditInterceptor
│       ├── security/         # GlobalTradeLoginModule (JAAS five-method lifecycle)
│       ├── exception/        # Six @ApplicationException subclasses with rollback semantics
│       └── util/             # DataInitializer (@Singleton @Startup seed data)
│
├── globaltrade-web/          # WAR — presentation and REST layer
│   └── src/main/java/com/global/
│       ├── servlet/          # Seven Servlets + SecurityFilter (@WebFilter("/*"))
│       └── rest/             # JAX-RS resources (ShipmentResource, VendorResource, InventoryResource)
│
├── globaltrade-ear/          # EAR — packages ejb + war, application.xml, context-root /GlobalTradeSCM
└── pom.xml                   # Parent POM — dependency management, plugin configuration
```

---

## Key Features

### EJB Timer Services
- Three declarative `@Schedule` persistent timers: shipment status monitoring (every 30s), inventory level alerts (every 60s), vendor performance evaluation (every 2 min)
- Programmatic `TimerService.createSingleActionTimer()` for customs deadline alerts calculated at runtime per shipment
- `@DependsOn("DataInitializer")` enforces startup ordering — timers fire only after seed data is available
- All timers write to `timer_logs` table with execution time, records processed, and success status

### Three-Interceptor Chain
Declared in `beans.xml` with `bean-discovery-mode="all"` in explicit execution order:

1. **ValidationInterceptor** (`@ValidatedInput`) — method-level null/blank parameter guard before JPA operations
2. **PerformanceInterceptor** (`@PerformanceMonitored`) — execution time measurement with WARN >= 500ms, SEVERE >= 2000ms SLA thresholds
3. **AuditInterceptor** (`@Audited`) — delegates to `AuditLogPersistenceBean` with `@REQUIRES_NEW` so audit records survive business transaction rollbacks

### Container-Managed Transactions
Five transaction attributes applied by scenario:

| Attribute | Applied To |
|---|---|
| `REQUIRED` | `createShipment`, `updateShipmentStatus`, `processCustomsClearance` |
| `REQUIRES_NEW` | `cancelShipment`, `AuditLogPersistenceBean` (audit independence) |
| `SUPPORTS` | `findAll`, `findById` (read-only, no TX overhead) |
| `NOT_SUPPORTED` | `findLowStockItems` (timer-initiated scan, avoids connection acquisition) |
| `REQUIRED` + `PESSIMISTIC_WRITE` | `deductStock` (exclusive row lock, prevents concurrent overselling) |

### Security Architecture
Three-layer defence in depth:
- **SecurityFilter** `@WebFilter("/*")` — session validation on every HTTP request
- **Programmatic `assertRole()`** — role enforcement in Servlet and EJB layers
- **JAAS GlobalTradeLoginModule** — full five-method lifecycle (`initialize`, `login`, `commit`, `abort`, `logout`) with SHA-256 password hashing and SQL injection prevention via `PreparedStatement`

### Role-Based Access Control

| Role | Permitted |
|---|---|
| `SYSTEM_ADMIN` | All operations |
| `LOGISTICS_COORDINATOR` | Create/update/cancel shipments, view all data |
| `CUSTOMS_AGENT` | Process customs clearance, update shipment status |
| `WAREHOUSE_MANAGER` | Inventory deduct/replenish, view shipments |
| `VENDOR_REPRESENTATIVE` | View own vendor data and assigned shipments |

### Exception Taxonomy

| Exception | `rollback` | Scenario |
|---|---|---|
| `CustomsComplianceException` | `true` | Invalid/missing customs declaration |
| `InsufficientInventoryException` | `true` | Stock deduction below zero |
| `VendorValidationException` | `true` | Invalid vendor input, caught at ValidationInterceptor |
| `ShipmentNotFoundException` | `false` | Lookup failure, no DB state changed |
| `UnauthorizedAccessException` | `false` | Role violation, servlet redirect |
| `SupplyChainException` | `true` | Base class, safe default |

---

## Prerequisites

- JDK 17
- GlassFish 7.0.21
- MySQL 8.0
- Maven 3.9+

---

## Database Setup

```sql
-- Run once before deployment
mysql -u root -p < database/schema.sql
```

Creates database `global_trade_db` with six tables: `users`, `shipments`, `vendors`, `inventory_items`, `audit_logs`, `timer_logs`. Seed data is injected automatically by `DataInitializer` on first EAR startup.

---

## Build and Deploy

```bash
# Build all modules
mvn clean package -DskipTests

# Deploy to GlassFish
asadmin deploy globaltrade-ear/target/GlobalTradeSCM.ear
```

Application is available at `http://localhost:8080/GlobalTradeSCM`.

Default admin credentials are seeded by `DataInitializer` on first startup.

---

## Running Tests

```bash
# Unit tests and integration tests (excluding Arquillian)
mvn test

# Arquillian container integration test (requires GlassFish running with EAR deployed)
mvn verify
```

### Test Coverage — 70 Tests

| Test Class | Tests | Coverage |
|---|---|---|
| `ShipmentManagementBeanTest` | 14 | create, updateStatus, processCustomsClearance, cancelShipment, findAll, findByStatus |
| `AuthenticationBeanTest` | 10 | login, assertRole (all 5 roles), register, inactive account rejection |
| `InventoryManagementBeanTest` | 7 | deductStock (PESSIMISTIC_WRITE), replenish, findLowStock |
| `SecurityPenetrationTest` | 18 | SQL injection (6 payloads), role escalation (5 boundaries), hash security |
| `ExceptionHandlingTest` | 14 | @ApplicationException rollback flags, HTTP status mapping |
| `InterceptorIntegrationTest` | 6 | ValidationInterceptor null/blank detection via Mockito InvocationContext |
| `ShipmentManagementIT` (Arquillian) | 1 | Full EAR container deployment, JPA persistence, REQUIRES_NEW audit verification |

---

## REST API

Base path: `/GlobalTradeSCM/api`

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/shipments` | List all shipments |
| `POST` | `/shipments` | Create shipment |
| `PUT` | `/shipments/{id}/status` | Update shipment status |
| `PUT` | `/shipments/{id}/customs` | Process customs clearance |
| `GET` | `/vendors` | List all vendors |
| `GET` | `/inventory` | List inventory items |
| `POST` | `/inventory/{id}/deduct` | Deduct stock |

---

## Architecture Notes

**Why EAR over single WAR:**
The EJB JAR's independent ClassLoader context prevents class loading conflicts during module updates. GlassFish EAR deployment allows the WAR to be redeployed without undeploying the EJB JAR. `application.xml` provides the `/GlobalTradeSCM` context root and module dependency declarations required for enterprise-grade deployment.

**REQUIRES_NEW Audit Pattern:**
`AuditLogPersistenceBean` uses `@TransactionAttribute(REQUIRES_NEW)` so audit records are committed in an independent transaction. A rolled-back business transaction (e.g. failed customs clearance) still produces a traceable audit entry — a mandatory requirement for international trade compliance.

---

## License

This project was developed as part of the Business Component Development II (JIAT/BCD II/EX/01) assessment at Java Institute for Advanced Technology / Birmingham City University.
