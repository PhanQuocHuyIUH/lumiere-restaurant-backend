-- =====================================================
-- V1: Initialize all database enums (final state).
-- =====================================================

CREATE TYPE staff_role_enum             AS ENUM ('MANAGER', 'WAITER', 'CASHIER', 'KITCHEN');
CREATE TYPE staff_status_enum           AS ENUM ('ACTIVE', 'INACTIVE');

CREATE TYPE menu_item_type_enum         AS ENUM ('SINGLE', 'COMBO');
CREATE TYPE combo_kind_enum             AS ENUM ('FIXED', 'PICK');

CREATE TYPE table_status_enum           AS ENUM ('AVAILABLE', 'OCCUPIED', 'RESERVED', 'CLEANING');
CREATE TYPE qr_code_status_enum         AS ENUM ('ACTIVE', 'DISABLED', 'ROTATING');
CREATE TYPE qr_session_status_enum      AS ENUM ('ACTIVE', 'EXPIRED', 'REVOKED');

CREATE TYPE order_status_enum           AS ENUM ('CREATED', 'CONFIRMED', 'PREPARING', 'READY', 'SERVED', 'PAID', 'CANCELLED');
CREATE TYPE revision_source_enum        AS ENUM ('CUSTOMER_QR', 'STAFF');
CREATE TYPE order_item_status_enum      AS ENUM ('PENDING', 'PREPARING', 'DONE', 'SERVED', 'CANCELLED');

CREATE TYPE kitchen_task_status_enum    AS ENUM ('CREATED', 'COOKING', 'DONE', 'CANCELLED');
CREATE TYPE kitchen_batch_status_enum   AS ENUM ('SUGGESTED', 'CONFIRMED', 'IN_PROGRESS', 'DONE');
CREATE TYPE batch_source_enum           AS ENUM ('AI', 'MANUAL');

-- QR_CODE = VNPay or VietQR QR; VNPAY_ATM = VNPay card swipe; CASH = direct cash
CREATE TYPE payment_method_enum         AS ENUM ('QR_CODE', 'VNPAY_ATM', 'CASH');
CREATE TYPE payment_provider_enum       AS ENUM ('VNPAY', 'VIETQR', 'CASH');
CREATE TYPE payment_status_enum         AS ENUM ('PENDING', 'SUCCESS', 'FAILED', 'REFUNDED');
CREATE TYPE webhook_status_enum         AS ENUM ('PENDING', 'PROCESSED', 'FAILED', 'DUPLICATE');
CREATE TYPE txn_type_enum               AS ENUM ('INITIATE', 'QUERY', 'REFUND', 'CANCEL');
CREATE TYPE txn_status_enum             AS ENUM ('INITIATED', 'SUCCESS', 'FAILED', 'TIMEOUT');
CREATE TYPE refund_status_enum          AS ENUM ('INITIATED', 'PENDING', 'SUCCESS', 'FAILED');

-- G = gram, ML = millilitre, UNIT = discrete unit
CREATE TYPE ingredient_unit_enum        AS ENUM ('G', 'ML', 'UNIT');
CREATE TYPE stock_txn_type_enum         AS ENUM ('IMPORT', 'ADJUSTMENT', 'MANUAL_REPORT');

CREATE TYPE support_request_status_enum AS ENUM ('CREATED', 'ASSIGNED', 'IN_PROGRESS', 'RESOLVED', 'CLOSED');
CREATE TYPE shift_status_enum           AS ENUM ('OPEN', 'CLOSED');
