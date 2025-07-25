-- Crear extensiones útiles
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Crear tabla inventory para gestionar el inventario de productos
CREATE TABLE IF NOT EXISTS inventory (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL UNIQUE,
    quantity INTEGER NOT NULL DEFAULT 0 CHECK (quantity >= 0),
    reserved_quantity INTEGER NOT NULL DEFAULT 0 CHECK (reserved_quantity >= 0),
    min_stock_level INTEGER NOT NULL DEFAULT 10,
    max_stock_level INTEGER NOT NULL DEFAULT 1000,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version INTEGER DEFAULT 0,
    CONSTRAINT chk_reserved_quantity CHECK (reserved_quantity <= quantity)
);

-- Crear tabla purchase_history para registrar el historial de compras
CREATE TABLE IF NOT EXISTS purchase_history (
    id BIGSERIAL PRIMARY KEY,
    purchase_id UUID NOT NULL DEFAULT uuid_generate_v4(),
    product_id BIGINT NOT NULL,
    quantity_purchased INTEGER NOT NULL CHECK (quantity_purchased > 0),
    unit_price DECIMAL(12,2) NOT NULL CHECK (unit_price > 0),
    total_price DECIMAL(12,2) NOT NULL CHECK (total_price > 0),
    customer_info JSONB,
    purchase_status VARCHAR(20) NOT NULL DEFAULT 'COMPLETED',
    purchase_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Crear tabla inventory_movements para auditoría de movimientos
CREATE TABLE IF NOT EXISTS inventory_movements (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL,
    movement_type VARCHAR(20) NOT NULL, -- STOCK_IN, STOCK_OUT, ADJUSTMENT, RESERVATION, RELEASE
    quantity_change INTEGER NOT NULL,
    previous_quantity INTEGER NOT NULL,
    new_quantity INTEGER NOT NULL,
    reference_id VARCHAR(100), -- ID de referencia (compra, ajuste, etc.)
    reason VARCHAR(200),
    created_by VARCHAR(100) DEFAULT 'SYSTEM',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Crear índices para optimizar consultas
CREATE INDEX IF NOT EXISTS idx_inventory_product_id ON inventory(product_id);
CREATE INDEX IF NOT EXISTS idx_inventory_quantity ON inventory(quantity);
CREATE INDEX IF NOT EXISTS idx_inventory_updated_at ON inventory(updated_at);

CREATE INDEX IF NOT EXISTS idx_purchase_history_product_id ON purchase_history(product_id);
CREATE INDEX IF NOT EXISTS idx_purchase_history_purchase_date ON purchase_history(purchase_date);
CREATE INDEX IF NOT EXISTS idx_purchase_history_status ON purchase_history(purchase_status);
CREATE INDEX IF NOT EXISTS idx_purchase_history_purchase_id ON purchase_history(purchase_id);

CREATE INDEX IF NOT EXISTS idx_inventory_movements_product_id ON inventory_movements(product_id);
CREATE INDEX IF NOT EXISTS idx_inventory_movements_type ON inventory_movements(movement_type);
CREATE INDEX IF NOT EXISTS idx_inventory_movements_created_at ON inventory_movements(created_at);

-- Trigger para actualizar updated_at automáticamente en inventory
CREATE OR REPLACE FUNCTION update_inventory_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    NEW.version = OLD.version + 1;
    RETURN NEW;
END;
$$ language 'plpgsql';

DROP TRIGGER IF EXISTS update_inventory_updated_at ON inventory;
CREATE TRIGGER update_inventory_updated_at
    BEFORE UPDATE ON inventory
    FOR EACH ROW
    EXECUTE FUNCTION update_inventory_updated_at();

-- Trigger para registrar movimientos de inventario automáticamente
CREATE OR REPLACE FUNCTION log_inventory_movement()
RETURNS TRIGGER AS $$
BEGIN
    -- Solo registrar si la cantidad cambió
    IF OLD.quantity IS DISTINCT FROM NEW.quantity THEN
        INSERT INTO inventory_movements (
            product_id,
            movement_type,
            quantity_change,
            previous_quantity,
            new_quantity,
            reason
        ) VALUES (
            NEW.product_id,
            CASE 
                WHEN NEW.quantity > OLD.quantity THEN 'STOCK_IN'
                WHEN NEW.quantity < OLD.quantity THEN 'STOCK_OUT'
                ELSE 'ADJUSTMENT'
            END,
            NEW.quantity - OLD.quantity,
            OLD.quantity,
            NEW.quantity,
            'Automatic inventory movement logging'
        );
    END IF;
    RETURN NEW;
END;
$$ language 'plpgsql';

DROP TRIGGER IF EXISTS log_inventory_movement_trigger ON inventory;
CREATE TRIGGER log_inventory_movement_trigger
    AFTER UPDATE ON inventory
    FOR EACH ROW
    EXECUTE FUNCTION log_inventory_movement();

-- Insertar datos de inventario iniciales
INSERT INTO inventory (product_id, quantity, min_stock_level, max_stock_level) VALUES 
    (1, 50, 5, 100),   -- Laptop Gaming
    (2, 200, 20, 500), -- Mouse Inalámbrico
    (3, 75, 10, 200),  -- Teclado Mecánico
    (4, 30, 5, 100),   -- Monitor 4K
    (5, 100, 15, 300)  -- Auriculares Gaming
ON CONFLICT (product_id) DO NOTHING;

-- Función para verificar disponibilidad de stock
CREATE OR REPLACE FUNCTION check_stock_availability(
    p_product_id BIGINT,
    p_quantity INTEGER
) RETURNS BOOLEAN AS $$
DECLARE
    available_quantity INTEGER;
BEGIN
    SELECT (quantity - reserved_quantity) INTO available_quantity
    FROM inventory
    WHERE product_id = p_product_id;
    
    RETURN COALESCE(available_quantity, 0) >= p_quantity;
END;
$$ LANGUAGE plpgsql;

-- Función para reservar stock
CREATE OR REPLACE FUNCTION reserve_stock(
    p_product_id BIGINT,
    p_quantity INTEGER
) RETURNS BOOLEAN AS $$
DECLARE
    current_available INTEGER;
BEGIN
    -- Verificar disponibilidad y reservar en una sola operación
    UPDATE inventory 
    SET reserved_quantity = reserved_quantity + p_quantity,
        updated_at = CURRENT_TIMESTAMP
    WHERE product_id = p_product_id 
      AND (quantity - reserved_quantity) >= p_quantity;
    
    RETURN FOUND;
END;
$$ LANGUAGE plpgsql;

-- Función para liberar stock reservado
CREATE OR REPLACE FUNCTION release_reserved_stock(
    p_product_id BIGINT,
    p_quantity INTEGER
) RETURNS BOOLEAN AS $$
BEGIN
    UPDATE inventory 
    SET reserved_quantity = GREATEST(0, reserved_quantity - p_quantity),
        updated_at = CURRENT_TIMESTAMP
    WHERE product_id = p_product_id;
    
    RETURN FOUND;
END;
$$ LANGUAGE plpgsql;

-- Función para confirmar compra (reduce stock físico y libera reserva)
CREATE OR REPLACE FUNCTION confirm_purchase(
    p_product_id BIGINT,
    p_quantity INTEGER
) RETURNS BOOLEAN AS $$
BEGIN
    UPDATE inventory 
    SET quantity = quantity - p_quantity,
        reserved_quantity = GREATEST(0, reserved_quantity - p_quantity),
        updated_at = CURRENT_TIMESTAMP
    WHERE product_id = p_product_id 
      AND quantity >= p_quantity
      AND reserved_quantity >= p_quantity;
    
    RETURN FOUND;
END;
$$ LANGUAGE plpgsql;

-- Función para obtener estadísticas de inventario
CREATE OR REPLACE FUNCTION get_inventory_statistics()
RETURNS TABLE(
    total_products BIGINT,
    products_in_stock BIGINT,
    products_out_of_stock BIGINT,
    products_low_stock BIGINT,
    total_quantity BIGINT,
    total_reserved BIGINT
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        COUNT(*) as total_products,
        COUNT(*) FILTER (WHERE quantity > 0) as products_in_stock,
        COUNT(*) FILTER (WHERE quantity = 0) as products_out_of_stock,
        COUNT(*) FILTER (WHERE quantity <= min_stock_level AND quantity > 0) as products_low_stock,
        COALESCE(SUM(quantity), 0) as total_quantity,
        COALESCE(SUM(reserved_quantity), 0) as total_reserved
    FROM inventory;
END;
$$ LANGUAGE plpgsql;

-- Mensaje de confirmación
DO $$
BEGIN
    RAISE NOTICE 'Base de datos de inventario inicializada correctamente';
END $$;