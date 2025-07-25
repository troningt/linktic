package com.example.inventory.util;

/**
 * Constantes para pruebas
 */
public final class TestConstants {

  public static final String TEST_PROFILE = "test";
  public static final String INTEGRATION_TEST_PROFILE = "integration-test";

  // IDs de prueba
  public static final Long TEST_PRODUCT_ID = 100L;
  public static final Long TEST_INVENTORY_ID = 1L;
  public static final String TEST_CUSTOMER_EMAIL = "customer@test.com";
  public static final String TEST_API_KEY = "test-api-key";

  // URLs de prueba
  public static final String TEST_PRODUCTS_SERVICE_URL = "http://localhost:8081";
  public static final String TEST_HEALTH_ENDPOINT = "/api/v1/actuator/health";
  public static final String TEST_PRODUCTS_ENDPOINT = "/api/v1/products/{productId}";

  // Topics de Kafka
  public static final String TEST_INVENTORY_EVENTS_TOPIC = "test-inventory-events";
  public static final String TEST_PURCHASE_EVENTS_TOPIC = "test-purchase-events";
  public static final String TEST_STOCK_ALERTS_TOPIC = "test-stock-alerts";

  // Cantidades de prueba
  public static final Integer TEST_INITIAL_QUANTITY = 100;
  public static final Integer TEST_RESERVED_QUANTITY = 10;
  public static final Integer TEST_MIN_STOCK_LEVEL = 5;
  public static final Integer TEST_MAX_STOCK_LEVEL = 500;
  public static final Integer TEST_PURCHASE_QUANTITY = 5;

  // Precios de prueba
  public static final String TEST_UNIT_PRICE = "29.99";
  public static final String TEST_TOTAL_PRICE = "149.95";

  // Timeouts de prueba
  public static final int TEST_CONNECT_TIMEOUT = 1000;
  public static final int TEST_READ_TIMEOUT = 2000;

  // Mensajes de prueba
  public static final String TEST_REASON = "Test reason";
  public static final String TEST_REFERENCE_ID = "REF-TEST-123";
  public static final String TEST_PRODUCT_NAME = "Test Product";
  public static final String TEST_PRODUCT_DESCRIPTION = "Test product description";

  private TestConstants() {
    // Utility class
  }
}
