CREATE DATABASE IF NOT EXISTS global_trade_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE global_trade_db;

CREATE TABLE IF NOT EXISTS users (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    username        VARCHAR(50)     NOT NULL,
    password_hash   VARCHAR(255)    NOT NULL COMMENT 'SHA-256 hash — upgrade to bcrypt in production',
    full_name       VARCHAR(100)    NOT NULL,
    email           VARCHAR(150)    NOT NULL,
    role            VARCHAR(50)     NOT NULL,
    active          BOOLEAN         NOT NULL DEFAULT TRUE,
    last_login      DATETIME        NULL,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT pk_users             PRIMARY KEY (id),
    CONSTRAINT uq_users_username    UNIQUE (username),
    CONSTRAINT uq_users_email       UNIQUE (email),
    CONSTRAINT chk_users_role       CHECK (role IN (
        'SYSTEM_ADMIN','LOGISTICS_COORDINATOR','CUSTOMS_AGENT',
        'WAREHOUSE_MANAGER','VENDOR_REPRESENTATIVE'
    ))
) ENGINE=InnoDB COMMENT='Supply chain personnel — RBAC security layer';

CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_role     ON users(role);
CREATE INDEX idx_users_active   ON users(active);


CREATE TABLE IF NOT EXISTS vendors (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    vendor_code         VARCHAR(20)     NOT NULL,
    name                VARCHAR(200)    NOT NULL,
    country             VARCHAR(100)    NOT NULL,
    contact_email       VARCHAR(150)    NULL,
    contact_phone       VARCHAR(30)     NULL,
    performance_score   DECIMAL(5,2)    NULL DEFAULT 100.00
        COMMENT 'KPI score 0-100; below 75 triggers vendor performance timer alert',
    active              BOOLEAN         NOT NULL DEFAULT TRUE,
    last_evaluated      DATETIME        NULL     COMMENT 'Set by VendorPerformanceEvaluator timer',
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT pk_vendors               PRIMARY KEY (id),
    CONSTRAINT uq_vendors_code          UNIQUE (vendor_code),
    CONSTRAINT chk_vendors_perf_score   CHECK (performance_score BETWEEN 0 AND 100)
) ENGINE=InnoDB COMMENT='Vendor master — carrier and supplier registry';

CREATE INDEX idx_vendors_country  ON vendors(country);
CREATE INDEX idx_vendors_active   ON vendors(active);
CREATE INDEX idx_vendors_perf     ON vendors(performance_score);


CREATE TABLE IF NOT EXISTS shipments (
    id                          BIGINT          NOT NULL AUTO_INCREMENT,
    tracking_number             VARCHAR(50)     NOT NULL,
    status                      VARCHAR(30)     NOT NULL DEFAULT 'PENDING',
    origin_country              VARCHAR(100)    NOT NULL,
    destination_country         VARCHAR(100)    NOT NULL,
    carrier_name                VARCHAR(100)    NULL,
    weight_kg                   DECIMAL(10,3)   NULL,
    declared_value              DECIMAL(15,2)   NULL,
    currency_code               CHAR(3)         NULL DEFAULT 'USD',
    estimated_delivery          DATETIME        NULL,
    actual_delivery             DATETIME        NULL,
    customs_cleared             BOOLEAN         NOT NULL DEFAULT FALSE,
    customs_declaration_number  VARCHAR(100)    NULL,
    description                 TEXT            NULL,
    vendor_id                   BIGINT          NULL,
    created_at                  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT pk_shipments         PRIMARY KEY (id),
    CONSTRAINT uq_shipments_tracking UNIQUE (tracking_number),
    CONSTRAINT fk_shipments_vendor  FOREIGN KEY (vendor_id)
        REFERENCES vendors(id) ON DELETE SET NULL,
    CONSTRAINT chk_shipments_status CHECK (status IN (
        'PENDING','IN_TRANSIT','CUSTOMS_HOLD','CUSTOMS_CLEARED',
        'DELIVERED','CANCELLED','DELAYED','RETURNED'
    )),
    CONSTRAINT chk_shipments_value  CHECK (declared_value IS NULL OR declared_value >= 0)
) ENGINE=InnoDB COMMENT='Core shipment entity — full lifecycle from PENDING to DELIVERED';

CREATE INDEX idx_shipments_status           ON shipments(status);
CREATE INDEX idx_shipments_tracking         ON shipments(tracking_number);
CREATE INDEX idx_shipments_estimated_del    ON shipments(estimated_delivery);
CREATE INDEX idx_shipments_customs_cleared  ON shipments(customs_cleared);
CREATE INDEX idx_shipments_origin           ON shipments(origin_country);
CREATE INDEX idx_shipments_vendor           ON shipments(vendor_id);


CREATE TABLE IF NOT EXISTS inventory_items (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    item_code           VARCHAR(50)     NOT NULL,
    name                VARCHAR(200)    NOT NULL,
    description         TEXT            NULL,
    quantity_on_hand    INT             NOT NULL DEFAULT 0,
    reorder_point       INT             NOT NULL DEFAULT 10,
    reorder_quantity    INT             NOT NULL DEFAULT 50,
    unit_price          DECIMAL(12,2)   NULL,
    warehouse_location  VARCHAR(100)    NULL,
    last_replenishment  DATETIME        NULL COMMENT 'Updated by InventoryReplenishmentMonitor timer',
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT pk_inventory             PRIMARY KEY (id),
    CONSTRAINT uq_inventory_code        UNIQUE (item_code),
    CONSTRAINT chk_inventory_qty        CHECK (quantity_on_hand >= 0),
    CONSTRAINT chk_inventory_reorder    CHECK (reorder_point >= 0)
) ENGINE=InnoDB COMMENT='Warehouse inventory — concurrent-safe via PESSIMISTIC_WRITE lock';

CREATE INDEX idx_inventory_code         ON inventory_items(item_code);
CREATE INDEX idx_inventory_qty          ON inventory_items(quantity_on_hand);
CREATE INDEX idx_inventory_reorder      ON inventory_items(reorder_point);


CREATE TABLE IF NOT EXISTS audit_logs (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    username        VARCHAR(50)     NOT NULL COMMENT 'Caller principal from SessionContext',
    action          VARCHAR(200)    NOT NULL,
    entity_type     VARCHAR(100)    NULL,
    entity_id       VARCHAR(100)    NULL,
    execution_time  BIGINT          NULL     COMMENT 'Method execution time in ms (PerformanceInterceptor)',
    success         BOOLEAN         NOT NULL DEFAULT TRUE,
    error_message   TEXT            NULL,
    ip_address      VARCHAR(45)     NULL,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_audit_logs PRIMARY KEY (id)
) ENGINE=InnoDB COMMENT='Immutable audit trail — REQUIRES_NEW ensures records survive TX rollback';

CREATE INDEX idx_audit_username     ON audit_logs(username);
CREATE INDEX idx_audit_action       ON audit_logs(action);
CREATE INDEX idx_audit_entity_type  ON audit_logs(entity_type);
CREATE INDEX idx_audit_created      ON audit_logs(created_at);
CREATE INDEX idx_audit_success      ON audit_logs(success);


CREATE TABLE IF NOT EXISTS timer_logs (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    timer_type          VARCHAR(100)    NOT NULL,
    description         TEXT            NULL,
    execution_time_ms   BIGINT          NULL,
    success             BOOLEAN         NOT NULL DEFAULT TRUE,
    records_processed   INT             NOT NULL DEFAULT 0,
    error_message       TEXT            NULL,
    executed_at         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_timer_logs PRIMARY KEY (id)
) ENGINE=InnoDB COMMENT='Timer service health log — tracks @Schedule and @Timeout executions';

CREATE INDEX idx_timer_type     ON timer_logs(timer_type);
CREATE INDEX idx_timer_success  ON timer_logs(success);
CREATE INDEX idx_timer_executed ON timer_logs(executed_at);


INSERT INTO users (username, password_hash, full_name, email, role) VALUES
('admin',     'ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94f',
 'System Administrator',    'admin@globaltrade.com',     'SYSTEM_ADMIN'),
('logistics', 'ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94f',
 'Sarah Connor',            'sconnor@globaltrade.com',   'LOGISTICS_COORDINATOR'),
('customs',   'ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94f',
 'James Customs',           'jcustoms@globaltrade.com',  'CUSTOMS_AGENT'),
('warehouse', 'ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94f',
 'Mike Warehouse',          'mwarehouse@globaltrade.com','WAREHOUSE_MANAGER'),
('vendor',    'ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94f',
 'Vendor Rep',              'vendor@acmecorp.com',       'VENDOR_REPRESENTATIVE');

INSERT INTO vendors (vendor_code, name, country, contact_email, contact_phone, performance_score) VALUES
('VND-001', 'AcmeCorp Logistics',      'US', 'contact@acme.com',    '+1-555-0100', 92.50),
('VND-002', 'GlobalShip Partners',     'CN', 'ops@globalship.cn',   '+86-21-5550200', 88.00),
('VND-003', 'EuroFreight GmbH',        'DE', 'info@eurofreight.de', '+49-30-5550300', 71.50);

INSERT INTO inventory_items
    (item_code, name, description, quantity_on_hand, reorder_point, reorder_quantity, unit_price, warehouse_location)
VALUES
('ITEM-001', 'Electronic Components Kit', 'Mixed SMD components for PCB assembly', 150, 20, 100, 45.99, 'WH-A-001'),
('ITEM-002', 'Industrial Bearings Set',   'Heavy-duty ball bearings 6200 series',  8,   10, 50,  12.50, 'WH-B-012'),
('ITEM-003', 'Chemical Solvents Pack',    'Industrial-grade cleaning solvents',    200, 30, 150, 28.75, 'WH-C-003'),
('ITEM-004', 'Safety Equipment Bundle',   'PPE kit for warehouse personnel',       5,   15, 75,  89.00, 'WH-D-007');

INSERT INTO shipments
    (tracking_number, status, origin_country, destination_country, carrier_name,
     weight_kg, declared_value, currency_code, estimated_delivery, customs_cleared, vendor_id)
VALUES
('GT-2026-000001', 'IN_TRANSIT',    'China',   'United States', 'GlobalShip Partners',
 125.500, 8500.00, 'USD', DATE_ADD(NOW(), INTERVAL 3 DAY),  FALSE, 2),
('GT-2026-000002', 'CUSTOMS_HOLD',  'Germany', 'United Kingdom','EuroFreight GmbH',
 45.200,  2200.00, 'EUR', DATE_ADD(NOW(), INTERVAL 1 DAY),  FALSE, 3),
('GT-2026-000003', 'DELIVERED',     'Japan',   'Australia',     'AcmeCorp Logistics',
 12.750,  950.00,  'USD', DATE_SUB(NOW(), INTERVAL 2 DAY),  TRUE,  1);
