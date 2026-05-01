-- Add enums for support request and shift management

CREATE SCHEMA IF NOT EXISTS support;
CREATE SCHEMA IF NOT EXISTS shift;

CREATE TYPE support_request_status_enum AS ENUM ('CREATED', 'ASSIGNED', 'IN_PROGRESS', 'RESOLVED', 'CLOSED');
CREATE TYPE shift_status_enum AS ENUM ('OPEN', 'CLOSED');
