package com.example.inventory.util;

import static com.example.inventory.util.TestConstants.TEST_CUSTOMER_EMAIL;
import static com.example.inventory.util.TestConstants.TEST_INITIAL_QUANTITY;
import static com.example.inventory.util.TestConstants.TEST_INVENTORY_ID;
import static com.example.inventory.util.TestConstants.TEST_MAX_STOCK_LEVEL;
import static com.example.inventory.util.TestConstants.TEST_MIN_STOCK_LEVEL;
import static com.example.inventory.util.TestConstants.TEST_PRODUCT_DESCRIPTION;
import static com.example.inventory.util.TestConstants.TEST_PRODUCT_ID;
import static com.example.inventory.util.TestConstants.TEST_PRODUCT_NAME;
import static com.example.inventory.util.TestConstants.TEST_PURCHASE_QUANTITY;
import static com.example.inventory.util.TestConstants.TEST_REASON;
import static com.example.inventory.util.TestConstants.TEST_REFERENCE_ID;
import static com.example.inventory.util.TestConstants.TEST_RESERVED_QUANTITY;
import static com.example.inventory.util.TestConstants.TEST_TOTAL_PRICE;
import static com.example.inventory.util.TestConstants.TEST_UNIT_PRICE;
import java.util.Map;
/**
 * Builders para objetos de prueba
 */
public final class TestDataBuilder {

  /**
   * Builder para ProductDto de prueba
   */
  public static class ProductDtoBuilder {
    private Long id = TEST_PRODUCT_ID;
    private String name = TEST_PRODUCT_NAME;
    private java.math.BigDecimal price = new java.math.BigDecimal(TEST_UNIT_PRICE);
    private String description = TEST_PRODUCT_DESCRIPTION;
    private Boolean active = true;

    public ProductDtoBuilder withId(Long id) {
      this.id = id;
      return this;
    }

    public ProductDtoBuilder withName(String name) {
      this.name = name;
      return this;
    }

    public ProductDtoBuilder withPrice(java.math.BigDecimal price) {
      this.price = price;
      return this;
    }

    public ProductDtoBuilder withDescription(String description) {
      this.description = description;
      return this;
    }

    public ProductDtoBuilder withActive(Boolean active) {
      this.active = active;
      return this;
    }

    public com.example.inventory.controller.dto.ProductDto build() {
      return com.example.inventory.controller.dto.ProductDto.builder()
          .id(id)
          .name(name)
          .price(price)
          .description(description)
          .active(active)
          .build();
    }
  }

  /**
   * Builder para Inventory de prueba
   */
  public static class InventoryBuilder {
    private Long id = TEST_INVENTORY_ID;
    private Long productId = TEST_PRODUCT_ID;
    private Integer quantity = TEST_INITIAL_QUANTITY;
    private Integer reservedQuantity = TEST_RESERVED_QUANTITY;
    private Integer minStockLevel = TEST_MIN_STOCK_LEVEL;
    private Integer maxStockLevel = TEST_MAX_STOCK_LEVEL;

    public InventoryBuilder withId(Long id) {
      this.id = id;
      return this;
    }

    public InventoryBuilder withProductId(Long productId) {
      this.productId = productId;
      return this;
    }

    public InventoryBuilder withQuantity(Integer quantity) {
      this.quantity = quantity;
      return this;
    }

    public InventoryBuilder withReservedQuantity(Integer reservedQuantity) {
      this.reservedQuantity = reservedQuantity;
      return this;
    }

    public InventoryBuilder withMinStockLevel(Integer minStockLevel) {
      this.minStockLevel = minStockLevel;
      return this;
    }

    public InventoryBuilder withMaxStockLevel(Integer maxStockLevel) {
      this.maxStockLevel = maxStockLevel;
      return this;
    }

    public com.example.inventory.model.Inventory build() {
      return com.example.inventory.model.Inventory.builder()
          .id(id)
          .productId(productId)
          .quantity(quantity)
          .reservedQuantity(reservedQuantity)
          .minStockLevel(minStockLevel)
          .maxStockLevel(maxStockLevel)
          .build();
    }
  }

  /**
   * Builder para PurchaseRequestDto de prueba
   */
  public static class PurchaseRequestBuilder {
    private Long productId = TEST_PRODUCT_ID;
    private Integer quantity = TEST_PURCHASE_QUANTITY;
    private Map<String, Object> customerInfo = Map.of("email", TEST_CUSTOMER_EMAIL);

    public PurchaseRequestBuilder withProductId(Long productId) {
      this.productId = productId;
      return this;
    }

    public PurchaseRequestBuilder withQuantity(Integer quantity) {
      this.quantity = quantity;
      return this;
    }

    public PurchaseRequestBuilder withCustomerInfo(Map<String, Object> customerInfo) {
      this.customerInfo = customerInfo;
      return this;
    }

    public com.example.inventory.controller.dto.PurchaseRequestDto build() {
      return com.example.inventory.controller.dto.PurchaseRequestDto.builder()
          .productId(productId)
          .quantity(quantity)
          .customerInfo(customerInfo)
          .build();
    }
  }

  /**
   * Builder para PurchaseHistory de prueba
   */
  public static class PurchaseHistoryBuilder {
    private Long id = 1L;
    private java.util.UUID purchaseId = java.util.UUID.randomUUID();
    private Long productId = TEST_PRODUCT_ID;
    private Integer quantityPurchased = TEST_PURCHASE_QUANTITY;
    private java.math.BigDecimal unitPrice = new java.math.BigDecimal(TEST_UNIT_PRICE);
    private java.math.BigDecimal totalPrice = new java.math.BigDecimal(TEST_TOTAL_PRICE);
    private com.example.inventory.model.PurchaseHistory.PurchaseStatus purchaseStatus =
        com.example.inventory.model.PurchaseHistory.PurchaseStatus.COMPLETED;
    private Map<String, Object> customerInfo = Map.of("email", TEST_CUSTOMER_EMAIL);
    private java.time.LocalDateTime purchaseDate = java.time.LocalDateTime.now();

    public PurchaseHistoryBuilder withId(Long id) {
      this.id = id;
      return this;
    }

    public PurchaseHistoryBuilder withPurchaseId(java.util.UUID purchaseId) {
      this.purchaseId = purchaseId;
      return this;
    }

    public PurchaseHistoryBuilder withProductId(Long productId) {
      this.productId = productId;
      return this;
    }

    public PurchaseHistoryBuilder withQuantityPurchased(Integer quantityPurchased) {
      this.quantityPurchased = quantityPurchased;
      return this;
    }

    public PurchaseHistoryBuilder withUnitPrice(java.math.BigDecimal unitPrice) {
      this.unitPrice = unitPrice;
      return this;
    }

    public PurchaseHistoryBuilder withTotalPrice(java.math.BigDecimal totalPrice) {
      this.totalPrice = totalPrice;
      return this;
    }

    public PurchaseHistoryBuilder withPurchaseStatus(com.example.inventory.model.PurchaseHistory.PurchaseStatus status) {
      this.purchaseStatus = status;
      return this;
    }

    public PurchaseHistoryBuilder withCustomerInfo(Map<String, Object> customerInfo) {
      this.customerInfo = customerInfo;
      return this;
    }

    public PurchaseHistoryBuilder withPurchaseDate(java.time.LocalDateTime purchaseDate) {
      this.purchaseDate = purchaseDate;
      return this;
    }

    public com.example.inventory.model.PurchaseHistory build() {
      return com.example.inventory.model.PurchaseHistory.builder()
          .id(id)
          .purchaseId(purchaseId)
          .productId(productId)
          .quantityPurchased(quantityPurchased)
          .unitPrice(unitPrice)
          .totalPrice(totalPrice)
          .purchaseStatus(purchaseStatus)
          .customerInfo(customerInfo)
          .purchaseDate(purchaseDate)
          .build();
    }
  }

  /**
   * Builder para UpdateInventoryRequestDto de prueba
   */
  public static class UpdateInventoryRequestBuilder {
    private Integer quantity = 75;
    private Integer minStockLevel = TEST_MIN_STOCK_LEVEL;
    private Integer maxStockLevel = TEST_MAX_STOCK_LEVEL;
    private String reason = TEST_REASON;
    private String referenceId = TEST_REFERENCE_ID;

    public UpdateInventoryRequestBuilder withQuantity(Integer quantity) {
      this.quantity = quantity;
      return this;
    }

    public UpdateInventoryRequestBuilder withMinStockLevel(Integer minStockLevel) {
      this.minStockLevel = minStockLevel;
      return this;
    }

    public UpdateInventoryRequestBuilder withMaxStockLevel(Integer maxStockLevel) {
      this.maxStockLevel = maxStockLevel;
      return this;
    }

    public UpdateInventoryRequestBuilder withReason(String reason) {
      this.reason = reason;
      return this;
    }

    public UpdateInventoryRequestBuilder withReferenceId(String referenceId) {
      this.referenceId = referenceId;
      return this;
    }

    public com.example.inventory.controller.dto.UpdateInventoryRequestDto build() {
      return com.example.inventory.controller.dto.UpdateInventoryRequestDto.builder()
          .quantity(quantity)
          .minStockLevel(minStockLevel)
          .maxStockLevel(maxStockLevel)
          .reason(reason)
          .referenceId(referenceId)
          .build();
    }
  }

  // Factory methods estáticos para crear builders
  public static ProductDtoBuilder aProductDto() {
    return new ProductDtoBuilder();
  }

  public static InventoryBuilder anInventory() {
    return new InventoryBuilder();
  }

  public static PurchaseRequestBuilder aPurchaseRequest() {
    return new PurchaseRequestBuilder();
  }

  public static PurchaseHistoryBuilder aPurchaseHistory() {
    return new PurchaseHistoryBuilder();
  }

  public static UpdateInventoryRequestBuilder anUpdateInventoryRequest() {
    return new UpdateInventoryRequestBuilder();
  }

  private TestDataBuilder() {
    // Utility class
  }
}
