-- Crear extensiones útiles
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Crear tabla products si no existe
CREATE TABLE IF NOT EXISTS products (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    price DECIMAL(12,2) NOT NULL CHECK (price > 0),
    description VARCHAR(500),
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version INTEGER DEFAULT 0
);

-- Crear índices para optimizar consultas
CREATE INDEX IF NOT EXISTS idx_product_name ON products(name);
CREATE INDEX IF NOT EXISTS idx_product_active ON products(active);
CREATE INDEX IF NOT EXISTS idx_product_price ON products(price);
CREATE INDEX IF NOT EXISTS idx_product_created_at ON products(created_at);

-- Trigger para actualizar updated_at automáticamente
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    NEW.version = OLD.version + 1;
    RETURN NEW;
END;
$$ language 'plpgsql';

DROP TRIGGER IF EXISTS update_products_updated_at ON products;
CREATE TRIGGER update_products_updated_at
    BEFORE UPDATE ON products
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Insertar datos de prueba
INSERT INTO products (name, price, description) VALUES 
    ('Laptop Gaming', 1299.99, 'Laptop de alto rendimiento para gaming'),
    ('Mouse Inalámbrico', 29.99, 'Mouse ergonómico con conexión Bluetooth'),
    ('Teclado Mecánico', 89.99, 'Teclado mecánico con retroiluminación RGB'),
    ('Monitor 4K', 399.99, 'Monitor 4K de 27 pulgadas'),
    ('Auriculares Gaming', 79.99, 'Auriculares con micrófono y sonido surround')
ON CONFLICT DO NOTHING;

-- Crear función para obtener estadísticas de productos
CREATE OR REPLACE FUNCTION get_product_statistics()
RETURNS TABLE(
    total_products BIGINT,
    active_products BIGINT,
    inactive_products BIGINT,
    min_price DECIMAL(12,2),
    max_price DECIMAL(12,2),
    avg_price DECIMAL(12,2)
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        COUNT(*) as total_products,
        COUNT(*) FILTER (WHERE active = true) as active_products,
        COUNT(*) FILTER (WHERE active = false) as inactive_products,
        MIN(price) as min_price,
        MAX(price) as max_price,
        AVG(price) as avg_price
    FROM products;
END;
$$ LANGUAGE plpgsql;


-- Mensaje de confirmación
DO $$
BEGIN
    RAISE NOTICE 'Base de datos de productos inicializada correctamente';
END $$;