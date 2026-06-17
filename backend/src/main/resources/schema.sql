-- ============================================================
-- Precision Machine Tool Lifecycle Management System - DDL
-- Target: Oracle 19c+
-- ============================================================

CREATE TABLE t_machine_tool (
    id              NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    rfid_code       VARCHAR2(64)  NOT NULL UNIQUE,
    tool_code       VARCHAR2(64)  NOT NULL,
    tool_name       VARCHAR2(128) NOT NULL,
    tool_type       VARCHAR2(64),
    specification   VARCHAR2(256),
    machine_id      VARCHAR2(64),
    machine_name    VARCHAR2(128),
    status          VARCHAR2(32)  DEFAULT 'IN_USE',
    total_cutting_hours NUMBER(10,2) DEFAULT 0,
    accumulated_wear NUMBER(10,4) DEFAULT 0,
    health_score    NUMBER(5,2)  DEFAULT 100,
    max_cutting_hours NUMBER(10,2) DEFAULT 500,
    max_wear_limit  NUMBER(10,4) DEFAULT 0.5000,
    installed_time  TIMESTAMP,
    last_scan_time  TIMESTAMP,
    created_by      VARCHAR2(64),
    created_time    TIMESTAMP DEFAULT SYSTIMESTAMP,
    updated_by      VARCHAR2(64),
    updated_time    TIMESTAMP DEFAULT SYSTIMESTAMP,
    is_deleted      NUMBER(1) DEFAULT 0
);

COMMENT ON TABLE t_machine_tool IS '机床刀具主表';
COMMENT ON COLUMN t_machine_tool.rfid_code IS 'RFID电子标签编码';
COMMENT ON COLUMN t_machine_tool.tool_code IS '刀具物料编码';
COMMENT ON COLUMN t_machine_tool.health_score IS '健康度百分比 0-100';
COMMENT ON COLUMN t_machine_tool.status IS 'IN_USE/SCRAPPED/REPLACED/PENDING_REPLACE';
COMMENT ON COLUMN t_machine_tool.accumulated_wear IS '累计磨损量(mm)';

CREATE TABLE t_scan_record (
    id              NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    rfid_code       VARCHAR2(64)  NOT NULL,
    tool_id         NUMBER        NOT NULL,
    scan_type       VARCHAR2(32)  NOT NULL,
    operator_id     VARCHAR2(64),
    operator_name   VARCHAR2(64),
    workstation_id  VARCHAR2(64),
    gateway_id      VARCHAR2(64),
    cutting_hours   NUMBER(10,2) DEFAULT 0,
    wear_value      NUMBER(10,4) DEFAULT 0,
    scan_time       TIMESTAMP DEFAULT SYSTIMESTAMP,
    remark          VARCHAR2(512)
);

COMMENT ON TABLE t_scan_record IS 'RFID扫描记录表';
COMMENT ON COLUMN t_scan_record.scan_type IS 'SCRAP/REPLACE/INSPECT';

CREATE TABLE t_tool_warning (
    id              NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tool_id         NUMBER        NOT NULL,
    rfid_code       VARCHAR2(64)  NOT NULL,
    warning_type    VARCHAR2(32)  NOT NULL,
    warning_level   VARCHAR2(16)  NOT NULL,
    health_score    NUMBER(5,2),
    message         VARCHAR2(512),
    is_acknowledged NUMBER(1) DEFAULT 0,
    acknowledged_by VARCHAR2(64),
    acknowledged_time TIMESTAMP,
    created_time    TIMESTAMP DEFAULT SYSTIMESTAMP
);

COMMENT ON TABLE t_tool_warning IS '刀具预警记录表';
COMMENT ON COLUMN t_tool_warning.warning_type IS 'HEALTH_LOW/WEAR_EXCEED/HOURS_EXCEED';
COMMENT ON COLUMN t_tool_warning.warning_level IS 'URGENT/WARNING/INFO';

CREATE TABLE t_procurement_request (
    id              NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    request_no      VARCHAR2(64)  NOT NULL UNIQUE,
    tool_id         NUMBER        NOT NULL,
    rfid_code       VARCHAR2(64)  NOT NULL,
    tool_code       VARCHAR2(64)  NOT NULL,
    tool_name       VARCHAR2(128),
    specification   VARCHAR2(256),
    quantity        NUMBER(6) DEFAULT 1,
    reason          VARCHAR2(512),
    erp_status      VARCHAR2(32) DEFAULT 'PENDING',
    erp_request_id  VARCHAR2(128),
    erp_response    VARCHAR2(1024),
    requested_by    VARCHAR2(64),
    requested_time  TIMESTAMP DEFAULT SYSTIMESTAMP,
    synced_time     TIMESTAMP,
    created_time    TIMESTAMP DEFAULT SYSTIMESTAMP
);

COMMENT ON TABLE t_procurement_request IS '采购申请表（自动提报ERP）';
COMMENT ON COLUMN t_procurement_request.erp_status IS 'PENDING/SUBMITTED/APPROVED/REJECTED/FAILED';

CREATE INDEX idx_tool_rfid ON t_machine_tool(rfid_code);
CREATE INDEX idx_tool_status ON t_machine_tool(status);
CREATE INDEX idx_tool_health ON t_machine_tool(health_score);
CREATE INDEX idx_scan_rfid ON t_scan_record(rfid_code);
CREATE INDEX idx_scan_time ON t_scan_record(scan_time);
CREATE INDEX idx_warning_tool ON t_tool_warning(tool_id);
CREATE INDEX idx_warning_ack ON t_tool_warning(is_acknowledged);
CREATE INDEX idx_proc_erp ON t_procurement_request(erp_status);

-- ============================================================
-- 以旧换新折价凭证 + 预算单
-- ============================================================

ALTER TABLE t_machine_tool ADD (
    original_price      NUMBER(12,2) DEFAULT 0,
    residual_value      NUMBER(12,2) DEFAULT 0,
    three_month_output  NUMBER(14,2) DEFAULT 0
);

COMMENT ON COLUMN t_machine_tool.original_price IS '刀具采购原价(元)';
COMMENT ON COLUMN t_machine_tool.residual_value IS '残余资产价值(元)';
COMMENT ON COLUMN t_machine_tool.three_month_output IS '近三个月切削产值(元)';

CREATE TABLE t_trade_in_voucher (
    id              NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    voucher_no      VARCHAR2(64)  NOT NULL UNIQUE,
    tool_id         NUMBER        NOT NULL,
    rfid_code       VARCHAR2(64)  NOT NULL,
    tool_code       VARCHAR2(64)  NOT NULL,
    tool_name       VARCHAR2(128),
    specification   VARCHAR2(256),
    original_price  NUMBER(12,2) DEFAULT 0,
    total_cutting_hours NUMBER(10,2) DEFAULT 0,
    three_month_output  NUMBER(14,2) DEFAULT 0,
    health_score    NUMBER(5,2),
    residual_value  NUMBER(12,2) DEFAULT 0,
    discount_rate   NUMBER(5,4)  DEFAULT 0,
    voucher_status  VARCHAR2(32)  DEFAULT 'PENDING',
    approve_by      VARCHAR2(64),
    approve_time    TIMESTAMP,
    reject_reason   VARCHAR2(512),
    supplier_status VARCHAR2(32)  DEFAULT 'PENDING',
    supplier_request_id VARCHAR2(128),
    supplier_response   VARCHAR2(1024),
    supplier_sync_time  TIMESTAMP,
    created_by      VARCHAR2(64),
    created_time    TIMESTAMP DEFAULT SYSTIMESTAMP
);

COMMENT ON TABLE t_trade_in_voucher IS '以旧换新折价凭证';
COMMENT ON COLUMN t_trade_in_voucher.voucher_no IS '凭证编号';
COMMENT ON COLUMN t_trade_in_voucher.residual_value IS '残余资产价值(元)';
COMMENT ON COLUMN t_trade_in_voucher.discount_rate IS '折价率 0-1';
COMMENT ON COLUMN t_trade_in_voucher.voucher_status IS 'PENDING/APPROVED/REJECTED';
COMMENT ON COLUMN t_trade_in_voucher.supplier_status IS '供应商推送状态 PENDING/SUBMITTED/FAILED/CONFIRMED';

CREATE TABLE t_tool_budget (
    id              NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    budget_no       VARCHAR2(64)  NOT NULL UNIQUE,
    trade_in_voucher_id NUMBER,
    tool_code       VARCHAR2(64)  NOT NULL,
    tool_name       VARCHAR2(128),
    specification   VARCHAR2(256),
    quantity        NUMBER(6) DEFAULT 1,
    unit_price      NUMBER(12,2) DEFAULT 0,
    total_amount    NUMBER(14,2) DEFAULT 0,
    subsidy_amount  NUMBER(12,2) DEFAULT 0,
    actual_payment  NUMBER(12,2) DEFAULT 0,
    budget_status   VARCHAR2(32)  DEFAULT 'ACTIVE',
    source_type     VARCHAR2(32)  DEFAULT 'TRADE_IN',
    created_by      VARCHAR2(64),
    created_time    TIMESTAMP DEFAULT SYSTIMESTAMP,
    expire_time     TIMESTAMP
);

COMMENT ON TABLE t_tool_budget IS '刀具预算单';
COMMENT ON COLUMN t_tool_budget.budget_no IS '预算单号';
COMMENT ON COLUMN t_tool_budget.subsidy_amount IS '以旧换新补贴金额(元)';
COMMENT ON COLUMN t_tool_budget.actual_payment IS '实际应付(元)';
COMMENT ON COLUMN t_tool_budget.budget_status IS 'ACTIVE/USED/EXPIRED/CANCELLED';

CREATE INDEX idx_voucher_tool ON t_trade_in_voucher(tool_id);
CREATE INDEX idx_voucher_status ON t_trade_in_voucher(voucher_status);
CREATE INDEX idx_budget_voucher ON t_tool_budget(trade_in_voucher_id);
CREATE INDEX idx_budget_status ON t_tool_budget(budget_status);
