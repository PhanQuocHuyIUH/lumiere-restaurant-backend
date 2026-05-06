-- =====================================================
-- V2: Initialize all database schemas.
-- =====================================================

CREATE SCHEMA IF NOT EXISTS identity;
CREATE SCHEMA IF NOT EXISTS menu;
CREATE SCHEMA IF NOT EXISTS table_mgmt;
-- table_mgmt also owns QR codes and QR session tables.
CREATE SCHEMA IF NOT EXISTS ordering;
CREATE SCHEMA IF NOT EXISTS kitchen;
CREATE SCHEMA IF NOT EXISTS payment;
CREATE SCHEMA IF NOT EXISTS analytics;
CREATE SCHEMA IF NOT EXISTS inventory;
CREATE SCHEMA IF NOT EXISTS support;
CREATE SCHEMA IF NOT EXISTS shift;
