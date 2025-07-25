# Microservicios - Sistema de Productos e Inventario

Este proyecto implementa una arquitectura de microservicios con servicios de productos e inventario, utilizando PostgreSQL, Apache Kafka y Docker Compose.

## 🏗️ Arquitectura

El sistema está compuesto por:

- **products-service**: Servicio de gestión de productos (Puerto 8081)
- **inventory-service**: Servicio de gestión de inventario (Puerto 8082)
- **products-db**: Base de datos PostgreSQL para productos
- **inventory-db**: Base de datos PostgreSQL para inventario
- **kafka**: Broker de mensajería Apache Kafka
- **zookeeper**: Coordinador para Kafka
- **kafka-ui**: Interfaz web para administrar Kafka (Puerto 8080)

## 📋 Prerrequisitos

- Docker
- Docker Compose
- Git

## 🚀 Configuración Inicial

### 1. Clonar el repositorio
```bash
git clone <url-del-repositorio>
cd <nombre-del-directorio>
```

### 2. Crear archivo de variables de entorno

Crea un archivo `.env` en la raíz del proyecto con las siguientes variables:

```env
# Configuración de Bases de Datos
PRODUCTS_DB_NAME=products_db
PRODUCTS_DB_USER=products_user
PRODUCTS_DB_PASSWORD=products_pass
PRODUCTS_DB_PORT=5432
PRODUCTS_DB_HOST=products-db

INVENTORY_DB_NAME=inventory_db
INVENTORY_DB_USER=inventory_user
INVENTORY_DB_PASSWORD=inventory_pass
INVENTORY_DB_EXTERNAL_PORT=5433
INVENTORY_DB_PORT=5432
INVENTORY_DB_HOST=inventory-db

# Configuración de Kafka
KAFKA_BROKER_ID=1
KAFKA_HOST=kafka
KAFKA_INTERNAL_PORT=29092
KAFKA_EXTERNAL_PORT=9092
KAFKA_ZOOKEEPER_CONNECT=zookeeper:2181
KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1

# Configuración de Zookeeper
ZOOKEEPER_CLIENT_PORT=2181
ZOOKEEPER_TICK_TIME=2000

# Configuración de Servicios
PRODUCTS_SERVICE_PORT=8081
PRODUCTS_SERVICE_HOST=products-service
INVENTORY_SERVICE_PORT=8082
SPRING_PROFILES_ACTIVE=docker

# APIs Keys
PRODUCTS_API_KEY=your-products-api-key
INVENTORY_API_KEY=your-inventory-api-key

# Configuración de Health Checks
HEALTH_CHECK_INTERVAL=30s
HEALTH_CHECK_TIMEOUT=10s
HEALTH_CHECK_RETRIES=5
HEALTH_CHECK_START_PERIOD=60s

# Configuración de Kafka UI
KAFKA_UI_PORT=8080
KAFKA_UI_CLUSTER_NAME=local

# Configuración de Red
NETWORK_SUBNET=172.20.0.0/16

# Rutas de Logs
LOGS_PRODUCTS_PATH=./logs/products
LOGS_INVENTORY_PATH=./logs/inventory
```

### 3. Crear directorios necesarios

```bash
mkdir -p logs/products
mkdir -p logs/inventory
mkdir -p infrastructure/postgres
```

### 4. Crear scripts de inicialización de base de datos

Crea los archivos SQL de inicialización:

**infrastructure/postgres/init-products-db.sql**
```sql
-- Inicialización de la base de datos de productos
CREATE TABLE IF NOT EXISTS products (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(10,2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

**infrastructure/postgres/init-inventory-db.sql**
```sql
-- Inicialización de la base de datos de inventario
CREATE TABLE IF NOT EXISTS inventory (
    id SERIAL PRIMARY KEY,
    product_id INTEGER NOT NULL,
    quantity INTEGER NOT NULL DEFAULT 0,
    reserved_quantity INTEGER NOT NULL DEFAULT 0,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

## 🔧 Comandos de Ejecución

### Levantar todos los servicios
```bash
docker-compose up -d
```

### Ver logs de todos los servicios
```bash
docker-compose logs -f
```

### Ver logs de un servicio específico
```bash
docker-compose logs -f products-service
docker-compose logs -f inventory-service
```

### Detener todos los servicios
```bash
docker-compose down
```

### Detener y eliminar volúmenes
```bash
docker-compose down -v
```

### Reconstruir servicios
```bash
docker-compose up -d --build
```

## 🔍 Verificación del Estado

### Health Checks
Los servicios incluyen health checks automáticos. Para verificar manualmente:

```bash
# Verificar productos service
curl http://localhost:8081/actuator/health

# Verificar inventory service
curl http://localhost:8082/actuator/health
```

### Acceso a las Interfaces

- **Products Service**: http://localhost:8081
- **Inventory Service**: http://localhost:8082  
- **Kafka UI**: http://localhost:8080
- **Products Database**: localhost:5432
- **Inventory Database**: localhost:5433

## 🗃️ Conexión a Bases de Datos

### Products Database
```bash
docker exec -it products-db psql -U products_user -d products_db
```

### Inventory Database  
```bash
docker exec -it inventory-db psql -U inventory_user -d inventory_db
```

## 📊 Monitoreo con Kafka UI

Accede a http://localhost:8080 para:
- Ver topics de Kafka
- Monitorear mensajes
- Gestionar configuraciones
- Ver métricas de rendimiento

## 🛠️ Troubleshooting

### Problemas Comunes

**Los servicios no inician correctamente:**
```bash
# Verificar logs
docker-compose logs

# Verificar estado de contenedores
docker-compose ps
```

**Error de conexión a base de datos:**
- Verificar que las variables de entorno estén correctas
- Asegurarse de que los health checks de las BD estén pasando

**Problemas con Kafka:**
```bash
# Reiniciar servicios de Kafka
docker-compose restart zookeeper kafka
```

**Limpiar y reiniciar completamente:**
```bash
docker-compose down -v
docker system prune -f
docker-compose up -d --build
```

## 📝 Estructura del Proyecto

```
.
├── docker-compose.yml
├── .env
├── README.md
├── products-service/
│   └── Dockerfile
├── inventory-service/
│   └── Dockerfile
├── infrastructure/
│   └── postgres/
│       ├── init-products-db.sql
│       └── init-inventory-db.sql
└── logs/
    ├── products/
    └── inventory/
```

## 🔒 Consideraciones de Seguridad

- Cambiar las contraseñas por defecto en producción
- Configurar API keys seguras
- Implementar autenticación y autorización apropiadas
- Usar secretos de Docker en lugar de variables de entorno para datos sensibles

## 📈 Escalabilidad

Para escalar servicios individualmente:
```bash
docker-compose up -d --scale products-service=3
docker-compose up -d --scale inventory-service=2
```

## 🤝 Contribución

1. Fork el proyecto
2. Crea una rama para tu feature
3. Commit tus cambios
4. Push a la rama
5. Abre un Pull Request