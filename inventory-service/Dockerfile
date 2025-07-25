# Dockerfile optimizado para el microservicio de inventario

# Etapa 1: Build
FROM gradle:8.4-jdk17-alpine AS builder

# Establecer directorio de trabajo
WORKDIR /app

# Copiar archivos de configuración de Gradle
COPY build.gradle settings.gradle ./
COPY gradle.properties* ./

# Descargar dependencias (para mejor uso del cache de Docker)
RUN gradle dependencies --no-daemon || true

# Copiar código fuente
COPY src ./src

# Compilar la aplicación
RUN gradle clean build -x test --no-daemon

# Etapa 2: Runtime
FROM openjdk:17-jdk-slim

# Información del mantenedor
LABEL maintainer="inventory-team@company.com"
LABEL version="1.0.0"
LABEL description="Inventory Microservice"

# Variables de entorno
ENV JAVA_OPTS=""
ENV SPRING_PROFILES_ACTIVE=docker
ENV APP_HOME=/app
ENV APP_USER=inventory
ENV APP_GROUP=inventory

# Crear usuario no root para mayor seguridad
RUN groupadd -r $APP_GROUP && \
    useradd -r -g $APP_GROUP -d $APP_HOME -s /sbin/nologin $APP_USER

# Instalar dependencias del sistema
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
    curl \
    ca-certificates \
    tzdata && \
    rm -rf /var/lib/apt/lists/* && \
    ln -sf /usr/share/zoneinfo/UTC /etc/localtime

# Crear directorios necesarios
RUN mkdir -p $APP_HOME/logs && \
    mkdir -p $APP_HOME/config && \
    chown -R $APP_USER:$APP_GROUP $APP_HOME

# Establecer directorio de trabajo
WORKDIR $APP_HOME

# Copiar el JAR desde la etapa de build
COPY --from=builder /app/build/libs/*.jar app.jar

# Cambiar permisos del JAR
RUN chown $APP_USER:$APP_GROUP app.jar

# Cambiar al usuario no root
USER $APP_USER

# Exponer el puerto de la aplicación
EXPOSE 8082

# Configurar health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8082/api/v1/actuator/health || exit 1

# Configurar el punto de entrada
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar app.jar"]

# Configuraciones adicionales de JVM para contenedores
ENV JAVA_OPTS="-XX:+UseG1GC \
               -XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=75.0 \
               -XX:+HeapDumpOnOutOfMemoryError \
               -XX:HeapDumpPath=/app/logs/ \
               -XX:+UseStringDeduplication \
               -XX:+OptimizeStringConcat \
               -Dfile.encoding=UTF-8 \
               -Duser.timezone=UTC \
               -Djava.awt.headless=true \
               -Dspring.backgroundpreinitializer.ignore=true"