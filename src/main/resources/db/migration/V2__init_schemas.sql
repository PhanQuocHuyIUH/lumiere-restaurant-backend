CREATE SCHEMA IF NOT EXISTS identity;
CREATE SCHEMA IF NOT EXISTS menu;
CREATE SCHEMA IF NOT EXISTS table_mgmt;
-- QR code and QR session management tables are stored under table_mgmt.
CREATE SCHEMA IF NOT EXISTS ordering;
-- Cross-module technical tables are stored under shared.
CREATE SCHEMA IF NOT EXISTS shared;
CREATE SCHEMA IF NOT EXISTS kitchen;
CREATE SCHEMA IF NOT EXISTS payment;
CREATE SCHEMA IF NOT EXISTS analytics;
