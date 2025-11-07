-- =============================================================================
-- Flyway Migration V4: Drop Old Demo Tables from V1
-- =============================================================================
-- This migration drops the demo tables created in V1 that conflict with
-- the authentication schema in V3.
-- =============================================================================

-- Drop tables in reverse dependency order
DROP TABLE IF EXISTS order_items;
DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS products;
DROP TABLE IF EXISTS users;
