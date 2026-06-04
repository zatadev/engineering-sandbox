-- Runs once on first PostgreSQL initialization.
-- Creates the orderdb database used by order-service.
SELECT 'CREATE DATABASE orderdb'
    WHERE NOT EXISTS (
    SELECT FROM pg_database WHERE datname = 'orderdb'
)\gexec
