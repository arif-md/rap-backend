-- =============================================================================
-- Flyway Migration V1: Initial Schema
-- =============================================================================
-- This migration creates the initial database schema for the RAP application.
-- It runs automatically on first startup (both Azure and local Docker).
--
-- Flyway tracks which migrations have run in the flyway_schema_history table.
-- =============================================================================

-- Example: Users table
CREATE TABLE users (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    username NVARCHAR(100) NOT NULL UNIQUE,
    email NVARCHAR(255) NOT NULL UNIQUE,
    created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    updated_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    INDEX idx_username (username),
    INDEX idx_email (email)
);

-- Example: Products table
CREATE TABLE products (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(255) NOT NULL,
    description NVARCHAR(MAX),
    price DECIMAL(19,4) NOT NULL CHECK (price >= 0),
    stock_quantity INT NOT NULL DEFAULT 0 CHECK (stock_quantity >= 0),
    created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    updated_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    INDEX idx_name (name)
);

-- Example: Orders table
CREATE TABLE orders (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    order_date DATETIME2 NOT NULL DEFAULT GETDATE(),
    total_amount DECIMAL(19,4) NOT NULL CHECK (total_amount >= 0),
    status NVARCHAR(50) NOT NULL DEFAULT 'PENDING',
    created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    updated_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_order_date (order_date),
    INDEX idx_status (status)
);

-- Example: Order Items table
CREATE TABLE order_items (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL CHECK (quantity > 0),
    unit_price DECIMAL(19,4) NOT NULL CHECK (unit_price >= 0),
    created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(id),
    INDEX idx_order_id (order_id),
    INDEX idx_product_id (product_id)
);

-- Insert sample data
INSERT INTO users (username, email) VALUES
    ('admin', 'admin@raptor.com'),
    ('john.doe', 'john.doe@example.com'),
    ('jane.smith', 'jane.smith@example.com');

INSERT INTO products (name, description, price, stock_quantity) VALUES
    ('Widget A', 'High-quality widget for general use', 29.99, 100),
    ('Widget B', 'Premium widget with advanced features', 49.99, 50),
    ('Gadget X', 'Innovative gadget for modern applications', 99.99, 25);

-- Success message
PRINT 'Initial schema created successfully!';
